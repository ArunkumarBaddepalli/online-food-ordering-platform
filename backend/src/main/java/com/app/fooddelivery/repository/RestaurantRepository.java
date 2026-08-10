package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    /**
     * The restaurant a given user account runs, if any.
     *
     * Deliberately "first": nothing stops an owner ending up with more than one
     * restaurant, and a plain findByOwnerId would throw rather than pick.
     */
    Optional<Restaurant> findFirstByOwnerIdOrderByIdAsc(Long ownerId);

    List<Restaurant> findByOwnerId(Long ownerId);
}
