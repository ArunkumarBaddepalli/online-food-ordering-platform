package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.RestaurantOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RestaurantOperatingHoursRepository extends JpaRepository<RestaurantOperatingHours, Long> {
    List<RestaurantOperatingHours> findByRestaurantId(Long restaurantId);
    Optional<RestaurantOperatingHours> findByRestaurantIdAndDayOfWeek(Long restaurantId, String dayOfWeek);
}
