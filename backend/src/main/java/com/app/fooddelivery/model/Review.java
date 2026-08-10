package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A customer's rating of one order.
 *
 * Tied to the order rather than just the restaurant, so a review can only come
 * from someone who actually ordered, and only once per order.
 */
@Entity
@Data
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    /** 1 to 5. */
    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

    /** The reviewer's name, so the list does not have to load each user. */
    private String authorName;
}
