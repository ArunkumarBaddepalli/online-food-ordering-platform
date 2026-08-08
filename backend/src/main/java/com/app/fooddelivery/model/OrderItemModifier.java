package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Junction table storing which modifiers were selected for each order item.
 * Allows tracking of customizations made to ordered items.
 */
@Entity
@Data
@Table(name = "order_item_modifier")
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    @Column(nullable = false)
    private Integer quantity = 1; // For quantity-based modifiers (e.g., "2x Extra Cheese")

    @Column(name = "price_at_order", nullable = false)
    private Double priceAtOrder; // Store the price adjustment at time of order

    @Column(name = "modifier_name", nullable = false)
    private String modifierName; // Store name in case modifier is deleted later
}
