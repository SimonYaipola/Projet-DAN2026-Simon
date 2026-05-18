package com.craftboard.server.service;

import com.craftboard.core.dto.BitjitaClaimImportResponse;
import com.craftboard.core.dto.BitjitaClaimSearchResponse;
import com.craftboard.core.dto.BitjitaClaimSummary;
import com.craftboard.core.dto.BitjitaMemberSummary;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.core.enums.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsable des appels externes vers l'API Bitjita.
 */
@Service
public class BitjitaClientService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final com.craftboard.server.repository.CityRepository cityRepository;
    private final com.craftboard.server.repository.AppUserRepository appUserRepository;
    private final PasswordService passwordService;

    public BitjitaClientService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${bitjita.base-url:https://bitjita.com}") String baseUrl,
            @Value("${bitjita.app-identifier:CraftBoard Projet-DAN2026}") String appIdentifier,
            com.craftboard.server.repository.CityRepository cityRepository,
            com.craftboard.server.repository.AppUserRepository appUserRepository,
            PasswordService passwordService) {
        this.baseUrl = baseUrl;
        this.cityRepository = cityRepository;
        this.appUserRepository = appUserRepository;
        this.passwordService = passwordService;
        this.restTemplate = restTemplateBuilder
                .defaultHeader("User-Agent", appIdentifier)
                .defaultHeader("x-app-identifier", appIdentifier)
                .setConnectTimeout(Duration.ofSeconds(8))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    public JsonNode status() {
        return get("/api/status");
    }

    public JsonNode searchClaims(String query, Integer page, Integer limit, String sort, String order, Integer regionId) {
        UriComponentsBuilder builder = uriBuilder("/api/claims")
                .queryParamIfPresent("q", optionalText(query))
                .queryParamIfPresent("page", optionalNumber(page))
                .queryParamIfPresent("limit", optionalNumber(limit))
                .queryParamIfPresent("sort", optionalText(sort))
                .queryParamIfPresent("order", optionalText(order))
                .queryParamIfPresent("regionId", optionalNumber(regionId));
        return get(builder.build(true).toUri());
    }

    public BitjitaClaimSearchResponse searchClaimSummaries(
            String query,
            Integer page,
            Integer limit,
            String sort,
            String order,
            Integer regionId) {
        JsonNode payload = searchClaims(query, page, limit, sort, order, regionId);
        List<BitjitaClaimSummary> claims = new ArrayList<>();
        payload.path("claims").forEach(node -> claims.add(toClaimSummary(node)));
        return new BitjitaClaimSearchResponse(claims, payload.path("count").asInt(claims.size()));
    }

    @Transactional
    public BitjitaClaimImportResponse importClaim(String id) {
        JsonNode claimPayload = claim(id);
        JsonNode memberPayload = claimMembers(id);
        JsonNode citizenPayload = claimCitizens(id);

        JsonNode claimNode = claimPayload.path("claim");
        BitjitaClaimSummary claim = toClaimSummary(claimNode);

        com.craftboard.server.entity.City city = cityRepository.findByCode(id)
                .orElseGet(com.craftboard.server.entity.City::new);
        city.setCode(id);
        city.setName(text(claimNode, "name", "Ville BitJita " + id));
        city.setApiUrl(baseUrl + "/claims/" + id);
        com.craftboard.server.entity.City savedCity = cityRepository.save(city);

        Map<String, BitjitaMemberSummary> membersByName = new LinkedHashMap<>();
        memberPayload.path("members").forEach(memberNode -> {
            BitjitaMemberSummary summary = toMemberSummary(memberNode, null);
            if (summary.userName() != null && !summary.userName().isBlank()) {
                membersByName.put(summary.userName(), summary);
                upsertUser(savedCity, summary);
            }
        });

        citizenPayload.path("citizens").forEach(citizenNode -> {
            String name = text(citizenNode, "userName", null);
            if (name != null && membersByName.containsKey(name)) {
                BitjitaMemberSummary existing = membersByName.get(name);
                membersByName.put(name, new BitjitaMemberSummary(
                        existing.playerEntityId(),
                        existing.userName(),
                        existing.inventoryPermission(),
                        existing.buildPermission(),
                        existing.officerPermission(),
                        existing.coOwnerPermission(),
                        nullableInt(citizenNode, "highestLevel"),
                        nullableInt(citizenNode, "totalLevel")
                ));
            }
        });

        CityResponse cityResponse = new CityResponse(
                savedCity.getId(),
                savedCity.getName(),
                savedCity.getApiUrl(),
                savedCity.getCode(),
                savedCity.getCreatedAt()
        );
        return new BitjitaClaimImportResponse(
                cityResponse,
                claim,
                new ArrayList<>(membersByName.values()),
                memberPayload.path("count").asInt(membersByName.size()),
                citizenPayload.path("count").asInt()
        );
    }

    public JsonNode claim(String id) {
        return get("/api/claims/" + id);
    }

    public JsonNode claimMembers(String id) {
        return get("/api/claims/" + id + "/members");
    }

    public JsonNode claimCitizens(String id) {
        return get("/api/claims/" + id + "/citizens");
    }

    public JsonNode claimInventories(String id) {
        return get("/api/claims/" + id + "/inventories");
    }

    public JsonNode claimBuildings(String id) {
        return get("/api/claims/" + id + "/buildings");
    }

    public JsonNode playerEquipment(String playerEntityId) {
        return get("/api/players/" + playerEntityId + "/equipment");
    }

    public JsonNode playerEquipmentPresets(String playerEntityId) {
        return get("/api/players/" + playerEntityId + "/equipment/presets");
    }

    public JsonNode playerInventories(String playerEntityId) {
        return get("/api/players/" + playerEntityId + "/inventories");
    }

    public JsonNode searchItems(String query) {
        return get(uriBuilder("/api/items")
                .queryParamIfPresent("q", optionalText(query))
                .build(true)
                .toUri());
    }

    public JsonNode item(Long id) {
        return get("/api/items/" + id);
    }

    public JsonNode searchCargo(String query) {
        return get(uriBuilder("/api/cargo")
                .queryParamIfPresent("q", optionalText(query))
                .build(true)
                .toUri());
    }

    private void upsertUser(com.craftboard.server.entity.City city, BitjitaMemberSummary summary) {
        java.util.Optional<com.craftboard.server.entity.AppUser> existingUser = appUserRepository
                .findByCityIdAndUsername(city.getId(), summary.userName());
        com.craftboard.server.entity.AppUser user = existingUser
                .orElseGet(com.craftboard.server.entity.AppUser::new);
        user.setCity(city);
        user.setUsername(summary.userName());
        if (existingUser.isEmpty()) {
            user.setRole(UserRole.CITIZEN);
            user.setDisplayName(summary.userName());
        } else if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(summary.userName());
        }
        user.setPlayerEntityId(summary.playerEntityId());
        if (user.getPasswordHash() == null) {
            user.setPasswordHash(passwordService.hash("bitjita-import"));
            user.setPasswordConfigured(false);
        }
        appUserRepository.save(user);
    }

    private BitjitaClaimSummary toClaimSummary(JsonNode node) {
        return new BitjitaClaimSummary(
                text(node, "entityId", null),
                text(node, "name", null),
                text(node, "regionName", null),
                nullableInt(node, "tier"),
                nullableInt(node, "numTiles"),
                firstText(node,
                        "ownerPlayerUsername",
                        "ownerUsername",
                        "leaderUsername",
                        "playerUsername",
                        "ownerName")
        );
    }

    private BitjitaMemberSummary toMemberSummary(JsonNode node, JsonNode citizenNode) {
        return new BitjitaMemberSummary(
                text(node, "playerEntityId", null),
                text(node, "userName", null),
                nullableInt(node, "inventoryPermission"),
                nullableInt(node, "buildPermission"),
                nullableInt(node, "officerPermission"),
                nullableInt(node, "coOwnerPermission"),
                citizenNode == null ? null : nullableInt(citizenNode, "highestLevel"),
                citizenNode == null ? null : nullableInt(citizenNode, "totalLevel")
        );
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field, null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private JsonNode get(String path) {
        return get(uriBuilder(path).build(true).toUri());
    }

    private JsonNode get(URI uri) {
        try {
            return restTemplate.getForObject(uri, JsonNode.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "BitJita request failed: " + uri + " - " + exception.getMessage(),
                    exception
            );
        }
    }

    private UriComponentsBuilder uriBuilder(String path) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
    }

    private java.util.Optional<String> optionalText(String value) {
        return value == null || value.isBlank()
                ? java.util.Optional.empty()
                : java.util.Optional.of(value);
    }

    private java.util.Optional<Integer> optionalNumber(Integer value) {
        return value == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(value);
    }
}
