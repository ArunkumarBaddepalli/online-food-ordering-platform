package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Represents a group of modifiers for a food item.
 * Examples: "Size", "Toppings", "Spice Level", "Add-ons"
 */
@Entity
@Data
@Table(name = "modifier_group")
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "Size", "Toppings", "Spice Level"

    @Column(name = "min_selection")
    private Integer minSelection; // Minimum number of selections required (0 if optional)

    @Column(name = "max_selection")
    private Integer maxSelection; // Maximum number of selections allowed (null for unlimited)

    @Column(nullable = false)
    private Boolean required = false; // Must the customer make a selection?

    @Column(name = "display_order")
    private Integer displayOrder = 0; // Order in which to display this group

    @ManyToOne
    @JoinColumn(name = "food_item_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private FoodItem foodItem; // Parent food item

    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Modifier> modifiers; // Available modifier options in this group
}
