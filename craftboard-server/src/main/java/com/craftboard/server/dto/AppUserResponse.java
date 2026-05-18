package com.craftboard.server.dto;

import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public class AppUserResponse {

    private Long id;
    private Long cityId;
    private String username;
    private String role;
    private String displayName;
    private LocalDateTime createdAt;

    public AppUserResponse() {
    }

    public AppUserResponse(
            Long id,
            Long cityId,
            String username,
            String role,
            String displayName,
            LocalDateTime createdAt) {
        this.id = id;
        this.cityId = cityId;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCityId() {
        return cityId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}