package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    Optional<Review> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    /** Average and count in one query, so the listing does not load every review. */
    @Query("SELECT r.restaurant.id, AVG(r.rating), COUNT(r) FROM Review r GROUP BY r.restaurant.id")
    List<Object[]> summariseByRestaurant();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.restaurant.id = :restaurantId")
    Double averageForRestaurant(@Param("restaurantId") Long restaurantId);
}
