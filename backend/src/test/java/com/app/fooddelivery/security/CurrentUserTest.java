package com.app.fooddelivery.security;

import com.app.fooddelivery.exception.ForbiddenException;
import com.app.fooddelivery.model.Order;
import com.app.fooddelivery.model.Restaurant;
import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.RestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The rules that decide who may touch what.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CurrentUserTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    private CurrentUser currentUser() {
        return new CurrentUser(restaurantRepository);
    }

    private void signedInAs(Long id, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(id, "u" + id + "@test.com", role), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Order orderBy(Long customerId, Long restaurantId) {
        User customer = new User();
        customer.setId(customerId);

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);

        Order order = new Order();
        order.setUser(customer);
        order.setRestaurant(restaurant);
        return order;
    }

    private void restaurantOwnedBy(Long restaurantId, Long ownerId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        if (ownerId != null) {
            User owner = new User();
            owner.setId(ownerId);
            restaurant.setOwner(owner);
        }
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
    }

    @Test
    @DisplayName("An anonymous caller is refused")
    void anonymousIsRefused() {
        SecurityContextHolder.clearContext();

        assertThat(currentUser().get()).isNull();
        assertThatThrownBy(() -> currentUser().requireSelfOrAdmin(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("You may reach your own records but not someone else's")
    void selfOnly() {
        signedInAs(4L, "USER");

        assertThatCode(() -> currentUser().requireSelfOrAdmin(4L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> currentUser().requireSelfOrAdmin(2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("another account");
    }

    @Test
    @DisplayName("An admin may act for anyone")
    void adminOverride() {
        signedInAs(1L, "ADMIN");

        assertThatCode(() -> currentUser().requireSelfOrAdmin(999L)).doesNotThrowAnyException();
        assertThatCode(() -> currentUser().requireOrderCustomer(orderBy(2L, 1L))).doesNotThrowAnyException();
        assertThatCode(() -> currentUser().requireRestaurantOwner(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Only the customer who placed an order counts as its customer")
    void orderCustomer() {
        signedInAs(4L, "USER");

        assertThatCode(() -> currentUser().requireOrderCustomer(orderBy(4L, 1L))).doesNotThrowAnyException();
        assertThatThrownBy(() -> currentUser().requireOrderCustomer(orderBy(2L, 1L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("An owner may manage their own restaurant only")
    void restaurantOwner() {
        signedInAs(5L, "RESTAURANT_OWNER");
        restaurantOwnedBy(1L, 5L);
        restaurantOwnedBy(2L, 9L);

        assertThatCode(() -> currentUser().requireRestaurantOwner(1L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> currentUser().requireRestaurantOwner(2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("do not manage");
    }

    @Test
    @DisplayName("A restaurant with no owner cannot be managed by anyone but an admin")
    void unownedRestaurant() {
        signedInAs(5L, "RESTAURANT_OWNER");
        restaurantOwnedBy(3L, null);

        assertThatThrownBy(() -> currentUser().requireRestaurantOwner(3L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Both the customer and the fulfilling restaurant may see an order")
    void orderParticipants() {
        Order order = orderBy(4L, 1L);
        restaurantOwnedBy(1L, 5L);

        signedInAs(4L, "USER");
        assertThatCode(() -> currentUser().requireOrderParticipant(order)).doesNotThrowAnyException();

        signedInAs(5L, "RESTAURANT_OWNER");
        assertThatCode(() -> currentUser().requireOrderParticipant(order)).doesNotThrowAnyException();

        signedInAs(9L, "USER");
        assertThatThrownBy(() -> currentUser().requireOrderParticipant(order))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("A customer cannot advance an order's status")
    void customerCannotDriveTheKitchen() {
        Order order = orderBy(4L, 1L);
        restaurantOwnedBy(1L, 5L);
        signedInAs(4L, "USER");

        assertThatThrownBy(() -> currentUser().requireOrderRestaurantOwner(order))
                .isInstanceOf(ForbiddenException.class);
    }
}
