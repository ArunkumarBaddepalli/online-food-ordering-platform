package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /** The restaurant a given user account runs, if any. */
    Optional<Restaurant> findByOwnerId(Long ownerId);
}
