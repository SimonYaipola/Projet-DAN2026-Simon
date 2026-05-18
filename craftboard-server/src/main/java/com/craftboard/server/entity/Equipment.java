package com.craftboard.server.entity;

import com.craftboard.core.enums.EquipmentCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entite JPA correspondant a une table de la base CraftBoard.
 */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pole_id")
    private Pole pole;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquipmentCategory category = EquipmentCategory.OTHER;

    @Column(length = 30)
    private String tier;

    @Column(length = 30)
    private String rarity;

    @Column(name = "bitjita_item_id", length = 64)
    private String bitjitaItemId;

    @Column(length = 80)
    private String slot;

    @Column(name = "icon_asset_name", length = 180)
    private String iconAssetName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "rarity_str", length = 80)
    private String rarityStr;

    @Column(name = "condition_value")
    private Integer conditionValue = 0;

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

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public Pole getPole() {
        return pole;
    }

    public void setPole(Pole pole) {
        this.pole = pole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getBitjitaItemId() {
        return bitjitaItemId;
    }

    public void setBitjitaItemId(String bitjitaItemId) {
        this.bitjitaItemId = bitjitaItemId;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getIconAssetName() {
        return iconAssetName;
    }

    public void setIconAssetName(String iconAssetName) {
        this.iconAssetName = iconAssetName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRarityStr() {
        return rarityStr;
    }

    public void setRarityStr(String rarityStr) {
        this.rarityStr = rarityStr;
    }

    public Integer getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(Integer conditionValue) {
        this.conditionValue = conditionValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
