package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    /** Incoming orders for a restaurant, newest first. */
    List<Order> findByRestaurantIdOrderByOrderDateDesc(Long restaurantId);
}
