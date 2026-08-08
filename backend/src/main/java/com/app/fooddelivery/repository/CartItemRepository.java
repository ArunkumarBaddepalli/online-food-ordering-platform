package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
