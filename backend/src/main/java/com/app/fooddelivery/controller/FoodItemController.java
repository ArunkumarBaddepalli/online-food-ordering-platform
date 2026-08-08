package com.app.fooddelivery.controller;

import com.app.fooddelivery.dto.StockUpdateRequest;
import com.app.fooddelivery.model.FoodItem;
import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.repository.FoodItemRepository;
import com.app.fooddelivery.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
public class FoodItemController {

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @PostMapping
    public ResponseEntity<FoodItem> createFoodItem(@RequestBody FoodItem foodItem, @RequestParam Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        foodItem.setRestaurant(restaurant);
        return ResponseEntity.ok(foodItemRepository.save(foodItem));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodItem> updateFoodItem(@PathVariable Long id, @RequestBody FoodItem updatedFood) {
        FoodItem food = foodItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Food item not found"));
        food.setName(updatedFood.getName());
        food.setDescription(updatedFood.getDescription());
        food.setPrice(updatedFood.getPrice());
        food.setImageUrl(updatedFood.getImageUrl());
        return ResponseEntity.ok(foodItemRepository.save(food));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFoodItem(@PathVariable Long id) {
        foodItemRepository.deleteById(id);
        return ResponseEntity.ok("Food item deleted");
    }

    /**
     * Update stock configuration for an item.
     * Used by restaurant owner to configure DAILY/MANUAL/UNLIMITED stock types.
     */
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody StockUpdateRequest req) {
        try {
            FoodItem food = foodItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Food item not found"));

            if (req.getInStock() != null) food.setInStock(req.getInStock());
            if (req.getStockQuantity() != null) food.setStockQuantity(req.getStockQuantity());
            if (req.getStockResetType() != null) food.setStockResetType(req.getStockResetType());
            if (req.getDailyStockLimit() != null) food.setDailyStockLimit(req.getDailyStockLimit());
            if (req.getDailyRestockTime() != null) food.setDailyRestockTime(req.getDailyRestockTime());
            if (req.getOosReason() != null) food.setOosReason(req.getOosReason());

            return ResponseEntity.ok(foodItemRepository.save(food));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Manually mark item as out of stock.
     */
    @PutMapping("/{id}/mark-oos")
    public ResponseEntity<?> markOOS(@PathVariable Long id, @RequestParam String reason) {
        try {
            FoodItem food = foodItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Food item not found"));
            food.setInStock(false);
            food.setOosReason(reason);
            food.setStockResetType("MANUAL");
            return ResponseEntity.ok(foodItemRepository.save(food));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Mark item as available again.
     */
    @PutMapping("/{id}/mark-available")
    public ResponseEntity<?> markAvailable(@PathVariable Long id) {
        try {
            FoodItem food = foodItemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Food item not found"));
            food.setInStock(true);
            food.setOosReason(null);
            food.setNextAvailableAt(null);
            return ResponseEntity.ok(foodItemRepository.save(food));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
