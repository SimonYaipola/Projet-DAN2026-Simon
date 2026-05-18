package com.craftboard.server.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "pole",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pole_city_name", columnNames = {"city_id", "name"})
        }
)
/**
 * Entite JPA correspondant a une table de la base CraftBoard.
 */
public class Pole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false, length = 120)
    private String name;

    public Long getId() {
        return id;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
