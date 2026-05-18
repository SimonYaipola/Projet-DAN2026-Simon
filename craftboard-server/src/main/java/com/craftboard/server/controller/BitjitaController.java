package com.craftboard.server.controller;

import com.craftboard.core.dto.BitjitaClaimImportResponse;
import com.craftboard.core.dto.BitjitaClaimSearchResponse;
import com.craftboard.server.service.BitjitaClientService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/bitjita")
public class BitjitaController {

    private final BitjitaClientService bitjitaClientService;

    public BitjitaController(BitjitaClientService bitjitaClientService) {
        this.bitjitaClientService = bitjitaClientService;
    }

    @GetMapping("/status")
    public JsonNode status() {
        return bitjitaClientService.status();
    }

    @GetMapping("/claims")
    public BitjitaClaimSearchResponse searchClaims(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "regionId", required = false) Integer regionId) {
        return bitjitaClientService.searchClaimSummaries(q, page, limit, sort, order, regionId);
    }

    @PostMapping("/claims/{id}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public BitjitaClaimImportResponse importClaim(@PathVariable(name = "id") String id) {
        return bitjitaClientService.importClaim(id);
    }

    @GetMapping("/claims/{id}")
    public JsonNode claim(@PathVariable(name = "id") String id) {
        return bitjitaClientService.claim(id);
    }

    @GetMapping("/claims/{id}/members")
    public JsonNode claimMembers(@PathVariable(name = "id") String id) {
        return bitjitaClientService.claimMembers(id);
    }

    @GetMapping("/claims/{id}/citizens")
    public JsonNode claimCitizens(@PathVariable(name = "id") String id) {
        return bitjitaClientService.claimCitizens(id);
    }

    @GetMapping("/claims/{id}/inventories")
    public JsonNode claimInventories(@PathVariable(name = "id") String id) {
        return bitjitaClientService.claimInventories(id);
    }

    @GetMapping("/claims/{id}/buildings")
    public JsonNode claimBuildings(@PathVariable(name = "id") String id) {
        return bitjitaClientService.claimBuildings(id);
    }

    @GetMapping("/items")
    public JsonNode searchItems(@RequestParam(name = "q", required = false) String q) {
        return bitjitaClientService.searchItems(q);
    }

    @GetMapping("/items/{id}")
    public JsonNode item(@PathVariable(name = "id") Long id) {
        return bitjitaClientService.item(id);
    }

    @GetMapping("/cargo")
    public JsonNode searchCargo(@RequestParam(name = "q", required = false) String q) {
        return bitjitaClientService.searchCargo(q);
    }
}
