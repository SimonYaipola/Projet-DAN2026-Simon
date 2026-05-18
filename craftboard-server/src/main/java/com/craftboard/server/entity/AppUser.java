package com.craftboard.server.entity;

import com.craftboard.core.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_city", columnNames = {"city_id", "username"})
        }
)
/**
 * Entite JPA correspondant a une table de la base CraftBoard.
 */
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.CITIZEN;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Column(name = "player_entity_id", length = 64)
    private String playerEntityId;

    @Column(name = "password_configured", nullable = false)
    private Boolean passwordConfigured = false;

    @Lob
    private byte[] avatar;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPlayerEntityId() {
        return playerEntityId;
    }

    public void setPlayerEntityId(String playerEntityId) {
        this.playerEntityId = playerEntityId;
    }

    public Boolean getPasswordConfigured() {
        return passwordConfigured;
    }

    public void setPasswordConfigured(Boolean passwordConfigured) {
        this.passwordConfigured = passwordConfigured;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
