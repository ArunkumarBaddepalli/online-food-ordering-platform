package com.app.fooddelivery.security;

import com.app.fooddelivery.exception.ForbiddenException;
import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.repository.RestaurantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Answers "who is calling, and are they allowed to touch this?".
 *
 * Every check goes through here so the rules live in one place rather than
 * being repeated, and slightly differently, in each controller.
 */
@Component
public class CurrentUser {

    private final RestaurantRepository restaurantRepository;

    public CurrentUser(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    /** The caller, or null when the request is anonymous. */
    public AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    public AuthenticatedUser require() {
        AuthenticatedUser user = get();
        if (user == null) {
            throw new ForbiddenException("You need to be signed in to do that.");
        }
        return user;
    }

    public Long requireId() {
        return require().id();
    }

    /** Admins may act on anyone's behalf; everyone else only on their own. */
    public void requireSelfOrAdmin(Long targetUserId) {
        AuthenticatedUser user = require();
        if (user.isAdmin()) {
            return;
        }
        if (!user.id().equals(targetUserId)) {
            throw new ForbiddenException("That belongs to another account.");
        }
    }

    /** The customer who placed the order, or an admin. */
    public void requireOrderCustomer(Order order) {
        AuthenticatedUser user = require();
        if (user.isAdmin()) {
            return;
        }
        Long ownerId = order.getUser() == null ? null : order.getUser().getId();
        if (!user.id().equals(ownerId)) {
            throw new ForbiddenException("That order belongs to another account.");
        }
    }

    /** The owner of the restaurant the order was placed with, or an admin. */
    public void requireOrderRestaurantOwner(Order order) {
        AuthenticatedUser user = require();
        if (user.isAdmin()) {
            return;
        }
        Long restaurantId = order.getRestaurant() == null ? null : order.getRestaurant().getId();
        if (restaurantId == null) {
            throw new ForbiddenException("That order is not linked to a restaurant.");
        }
        requireRestaurantOwner(restaurantId);
    }

    /** The owner of this specific restaurant, or an admin. */
    public void requireRestaurantOwner(Long restaurantId) {
        AuthenticatedUser user = require();
        if (user.isAdmin()) {
            return;
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ForbiddenException("Restaurant not found."));

        Long ownerId = restaurant.getOwner() == null ? null : restaurant.getOwner().getId();
        if (!user.id().equals(ownerId)) {
            throw new ForbiddenException("You do not manage that restaurant.");
        }
    }

    /** Either the customer who ordered or the restaurant fulfilling it. */
    public void requireOrderParticipant(Order order) {
        AuthenticatedUser user = require();
        if (user.isAdmin()) {
            return;
        }

        Long customerId = order.getUser() == null ? null : order.getUser().getId();
        if (user.id().equals(customerId)) {
            return;
        }

        requireOrderRestaurantOwner(order);
    }
}
