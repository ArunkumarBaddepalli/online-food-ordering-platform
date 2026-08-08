package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

/**
 * Junction table for storing modifiers selected in cart items.
 * Temporary storage before order is placed.
 */
@Entity
@Data
@Table(name = "cart_item_modifier")
public class CartItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_item_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private CartItem cartItem;

    @ManyToOne
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    @Column(nullable = false)
    private Integer quantity = 1;
}
