package com.craftboard.server.entity;

import jakarta.persistence.*;

/**
 * Entite JPA correspondant a une table de la base CraftBoard.
 */
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CraftOrder order;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 30)
    private String tier;

    @Column(nullable = false, length = 30)
    private String rarity;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "tool_type", length = 30)
    private String toolType;

    @Column(name = "equipment_type", length = 30)
    private String equipmentType;

    @Column(name = "armor_material", length = 30)
    private String armorMaterial;

    public Long getId() {
        return id;
    }

    public CraftOrder getOrder() {
        return order;
    }

    public void setOrder(CraftOrder order) {
        this.order = order;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getArmorMaterial() {
        return armorMaterial;
    }

    public void setArmorMaterial(String armorMaterial) {
        this.armorMaterial = armorMaterial;
    }
}
