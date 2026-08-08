package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.FoodItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByRestaurantId(Long restaurantId);

    List<FoodItem> findByRestaurantIdAndIsBestSellerTrue(Long restaurantId);

    List<FoodItem> findByStockResetType(String stockResetType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FoodItem f WHERE f.id = :id")
    Optional<FoodItem> findByIdWithLock(@Param("id") Long id);
}
