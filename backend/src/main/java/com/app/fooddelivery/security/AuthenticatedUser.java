package com.app.fooddelivery.security;

/** The caller identified by the token on the current request. */
public record AuthenticatedUser(Long id, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isRestaurantOwner() {
        return "RESTAURANT_OWNER".equals(role);
    }
}
