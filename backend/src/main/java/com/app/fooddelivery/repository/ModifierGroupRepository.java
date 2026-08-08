package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {
    List<ModifierGroup> findByFoodItemId(Long foodItemId);
}
