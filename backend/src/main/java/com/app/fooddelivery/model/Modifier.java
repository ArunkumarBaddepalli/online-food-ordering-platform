package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents an individual modifier option within a group.
 * Examples: "Large", "Extra Cheese", "Spicy", "No Onions"
 */
@Entity
@Data
@Table(name = "modifier")
public class Modifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Large", "Extra Cheese", "Medium Spicy"

    @Column(name = "price_adjustment", nullable = false)
    private Double priceAdjustment = 0.0; // Additional cost (can be negative for discounts)

    @Column(nullable = false)
    private Boolean available = true; // Is this modifier currently available?

    @Column(name = "display_order")
    private Integer displayOrder = 0; // Order in which to display this modifier

    @Column(length = 500)
    private String description; // Optional description of the modifier

    @ManyToOne
    @JoinColumn(name = "modifier_group_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ModifierGroup modifierGroup; // Parent modifier group
}
