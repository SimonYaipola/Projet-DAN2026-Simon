package com.craftboard.desktop.service;

import com.craftboard.core.dto.BitjitaClaimImportResponse;
import com.craftboard.core.dto.BitjitaClaimSearchResponse;
import com.craftboard.core.dto.AuthLoginRequest;
import com.craftboard.core.dto.AuthLoginResponse;
import com.craftboard.core.dto.AccountActivationRequest;
import com.craftboard.core.dto.ActivityLogResponse;
import com.craftboard.core.dto.AdminUserUpdateRequest;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.core.dto.CityResetRequest;
import com.craftboard.core.dto.CityResetResponse;
import com.craftboard.core.dto.CraftOrderRequest;
import com.craftboard.core.dto.CraftOrderResponse;
import com.craftboard.core.dto.OrderActionRequest;
import com.craftboard.core.dto.PlayerEquipmentResponse;
import com.craftboard.core.dto.PoleResponse;
import com.craftboard.core.dto.QuestDeliveryRequest;
import com.craftboard.core.dto.QuestRequest;
import com.craftboard.core.dto.QuestResponse;
import com.craftboard.core.dto.SetupAdminRequest;
import com.craftboard.core.dto.UserUpdateRequest;
import com.craftboard.core.dto.UserResponse;
import com.craftboard.core.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Centralise les appels HTTP du desktop vers l'API Spring Boot.
 */
public class ApiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final String REMEMBERED_SESSION_KEY = "rememberedSession";
    private static final String SERVER_URL_KEY = "serverUrl";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Preferences preferences = Preferences.userNodeForPackage(ApiService.class);

    private String baseUrl = DEFAULT_BASE_URL;
    private AuthLoginResponse currentSession;

    public ApiService() {
        // Les preferences gardent le serveur choisi et la session entre deux ouvertures.
        baseUrl = preferences.get(SERVER_URL_KEY, DEFAULT_BASE_URL);
        currentSession = loadRememberedSession();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public AuthLoginResponse getCurrentSession() {
        return currentSession;
    }

    public CityResponse getCurrentCity() {
        return currentSession == null ? null : currentSession.city();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String testAndSaveBaseUrl(String newBaseUrl) throws IOException, InterruptedException {
        String previousBaseUrl = baseUrl;
        setBaseUrl(newBaseUrl);
        try {
            String health = getHealth();
            preferences.put(SERVER_URL_KEY, baseUrl);
            return health;
        } catch (IOException | InterruptedException exception) {
            baseUrl = previousBaseUrl;
            throw exception;
        }
    }

    public String getHealth() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/health"))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    public BitjitaClaimSearchResponse searchBitjitaClaims(String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/bitjita/claims?q=" + encodedQuery + "&page=1&limit=20&sort=name&order=asc"))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        String body = send(request);
        return objectMapper.readValue(body, BitjitaClaimSearchResponse.class);
    }

    public BitjitaClaimImportResponse importBitjitaClaim(String entityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/bitjita/claims/" + entityId + "/import"))
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        String body = send(request);
        return objectMapper.readValue(body, BitjitaClaimImportResponse.class);
    }

    public List<CityResponse> getCities() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/cities?importedOnly=true"))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        String body = send(request);
        return Arrays.asList(objectMapper.readValue(body, CityResponse[].class));
    }

    public CityResetResponse resetCity(Long cityId, String adminPassword) throws IOException, InterruptedException {
        CityResetRequest payload = new CityResetRequest(adminPassword);
        HttpRequest request = jsonPost(baseUrl + "/api/cities/" + cityId + "/reset", payload, REQUEST_TIMEOUT);
        return objectMapper.readValue(send(request), CityResetResponse.class);
    }

    public AuthLoginResponse createInitialAdmin(Long cityId, String username, String password, String displayName)
            throws IOException, InterruptedException {
        SetupAdminRequest payload = new SetupAdminRequest(username, password, displayName);
        HttpRequest request = jsonPost(baseUrl + "/api/auth/cities/" + cityId + "/initial-admin", payload, REQUEST_TIMEOUT);
        currentSession = objectMapper.readValue(send(request), AuthLoginResponse.class);
        rememberCurrentSession();
        return currentSession;
    }

    public AuthLoginResponse login(Long cityId, String username, String password) throws IOException, InterruptedException {
        AuthLoginRequest payload = new AuthLoginRequest(cityId, username, password);
        HttpRequest request = jsonPost(baseUrl + "/api/auth/login", payload, REQUEST_TIMEOUT);
        currentSession = objectMapper.readValue(send(request), AuthLoginResponse.class);
        rememberCurrentSession();
        return currentSession;
    }

    public AuthLoginResponse activateAccount(Long cityId, String username, String password, String displayName)
            throws IOException, InterruptedException {
        AccountActivationRequest payload = new AccountActivationRequest(cityId, username, password, displayName);
        HttpRequest request = jsonPost(baseUrl + "/api/auth/activate", payload, REQUEST_TIMEOUT);
        currentSession = objectMapper.readValue(send(request), AuthLoginResponse.class);
        rememberCurrentSession();
        return currentSession;
    }

    public List<UserResponse> getUsersByCity(Long cityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/users/city/" + cityId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        String body = send(request);
        return Arrays.asList(objectMapper.readValue(body, UserResponse[].class));
    }

    public UserResponse adminUpdateUser(Long userId, UserRole role, String password, String displayName)
            throws IOException, InterruptedException {
        AdminUserUpdateRequest payload = new AdminUserUpdateRequest(role, password, displayName);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/users/" + userId + "/admin"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        return objectMapper.readValue(send(request), UserResponse.class);
    }

    public UserResponse updateCurrentUserProfile(String displayName, String password)
            throws IOException, InterruptedException {
        if (currentSession == null) {
            throw new IOException("No active session");
        }
        UserResponse user = currentSession.user();
        UserUpdateRequest payload = new UserUpdateRequest(
                user.cityId(),
                user.username(),
                password,
                user.role(),
                displayName
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/users/" + user.id()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        UserResponse updatedUser = objectMapper.readValue(send(request), UserResponse.class);
        currentSession = new AuthLoginResponse(updatedUser, currentSession.city());
        rememberCurrentSession();
        return updatedUser;
    }

    public PlayerEquipmentResponse getBitjitaEquippedItems(Long userId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/equipments/users/" + userId + "/bitjita-equipped"))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return objectMapper.readValue(send(request), PlayerEquipmentResponse.class);
    }

    public List<PoleResponse> getPolesByCity(Long cityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/poles?cityId=" + cityId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return Arrays.asList(objectMapper.readValue(send(request), PoleResponse[].class));
    }

    public List<CraftOrderResponse> getOrdersByCity(Long cityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/orders?cityId=" + cityId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return Arrays.asList(objectMapper.readValue(send(request), CraftOrderResponse[].class));
    }

    public CraftOrderResponse createOrder(CraftOrderRequest payload) throws IOException, InterruptedException {
        HttpRequest request = jsonPost(baseUrl + "/api/orders", payload, REQUEST_TIMEOUT);
        return objectMapper.readValue(send(request), CraftOrderResponse.class);
    }

    public CraftOrderResponse assignOrder(Long orderId, Long userId) throws IOException, InterruptedException {
        return orderAction(orderId, "/assign", userId, null);
    }

    public CraftOrderResponse completeOrder(Long orderId, Long userId, String notes) throws IOException, InterruptedException {
        return orderAction(orderId, "/complete", userId, notes);
    }

    public CraftOrderResponse cancelOrder(Long orderId, Long userId, String notes) throws IOException, InterruptedException {
        return orderAction(orderId, "/cancel", userId, notes);
    }

    public CraftOrderResponse reopenOrder(Long orderId, Long userId, String notes) throws IOException, InterruptedException {
        return orderAction(orderId, "/reopen", userId, notes);
    }

    public CraftOrderResponse updateOrderNotes(Long orderId, Long userId, String notes) throws IOException, InterruptedException {
        OrderActionRequest payload = new OrderActionRequest(userId, notes);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/orders/" + orderId + "/notes"))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        return objectMapper.readValue(send(request), CraftOrderResponse.class);
    }

    private CraftOrderResponse orderAction(Long orderId, String actionPath, Long userId, String notes)
            throws IOException, InterruptedException {
        OrderActionRequest payload = new OrderActionRequest(userId, notes);
        HttpRequest request = jsonPost(baseUrl + "/api/orders/" + orderId + actionPath, payload, REQUEST_TIMEOUT);
        return objectMapper.readValue(send(request), CraftOrderResponse.class);
    }

    public List<QuestResponse> getQuestsByCity(Long cityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/quests?cityId=" + cityId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return Arrays.asList(objectMapper.readValue(send(request), QuestResponse[].class));
    }

    public List<ActivityLogResponse> getActivityLogsByCity(Long cityId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/activity-logs?cityId=" + cityId))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return Arrays.asList(objectMapper.readValue(send(request), ActivityLogResponse[].class));
    }

    public QuestResponse createQuest(QuestRequest payload) throws IOException, InterruptedException {
        HttpRequest request = jsonPost(baseUrl + "/api/quests", payload, REQUEST_TIMEOUT);
        return objectMapper.readValue(send(request), QuestResponse.class);
    }

    public QuestResponse deliverQuest(Long questId, Long userId, Integer deliveredQuantity)
            throws IOException, InterruptedException {
        QuestDeliveryRequest payload = new QuestDeliveryRequest(userId, deliveredQuantity);
        HttpRequest request = jsonPost(baseUrl + "/api/quests/" + questId + "/deliveries", payload, REQUEST_TIMEOUT);
        return objectMapper.readValue(send(request), QuestResponse.class);
    }

    public void clearSession() {
        currentSession = null;
    }

    public void forgetRememberedSession() {
        currentSession = null;
        preferences.remove(REMEMBERED_SESSION_KEY);
    }

    private AuthLoginResponse loadRememberedSession() {
        String rememberedSession = preferences.get(REMEMBERED_SESSION_KEY, "");
        if (rememberedSession.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rememberedSession, AuthLoginResponse.class);
        } catch (IOException exception) {
            preferences.remove(REMEMBERED_SESSION_KEY);
            return null;
        }
    }

    private void rememberCurrentSession() {
        if (currentSession == null) {
            preferences.remove(REMEMBERED_SESSION_KEY);
            return;
        }
        try {
            preferences.put(REMEMBERED_SESSION_KEY, objectMapper.writeValueAsString(currentSession));
        } catch (IOException exception) {
            preferences.remove(REMEMBERED_SESSION_KEY);
        }
    }

    private HttpRequest jsonPost(String uri, Object body, Duration timeout) throws IOException {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private String normalizeBaseUrl(String value) {
        // Normalise l'URL pour construire les routes API sans double slash.
        String normalized = value == null || value.isBlank() ? DEFAULT_BASE_URL : value.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
