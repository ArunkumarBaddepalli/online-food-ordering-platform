package com.app.fooddelivery.controller;

import com.app.fooddelivery.dto.CartValidationResponse;
import com.app.fooddelivery.model.Cart;
import com.app.fooddelivery.security.CurrentUser;
import com.app.fooddelivery.service.CartService;
import com.app.fooddelivery.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CurrentUser currentUser;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long userId, @RequestParam Long foodId,
            @RequestParam int quantity, @RequestParam(required = false) List<Long> modifierIds) {
        currentUser.requireSelfOrAdmin(userId);
        try {
            return ResponseEntity.ok(cartService.addToCart(userId, foodId, quantity, modifierIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long itemId, @RequestParam int quantity) {
        requireOwnCartItem(itemId);
        try {
            return ResponseEntity.ok(cartService.updateCartItemQuantity(itemId, quantity));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<String> removeCartItem(@PathVariable Long itemId) {
        requireOwnCartItem(itemId);
        cartItemRepository.deleteById(itemId);
        return ResponseEntity.ok("Item removed from cart");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }

    @PostMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestParam Long userId, @RequestParam Long orderId) {
        currentUser.requireSelfOrAdmin(userId);
        try {
            return ResponseEntity.ok(cartService.reorder(userId, orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Validates cart items for OOS status before checkout.
     */
    @GetMapping("/{userId}/validate")
    public ResponseEntity<CartValidationResponse> validateCart(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(cartService.validateCart(userId));
    }

    /** Cart item routes have no userId in the path, so look it up. */
    private void requireOwnCartItem(Long itemId) {
        Long ownerId = cartItemRepository.findById(itemId)
                .map(item -> item.getCart() == null || item.getCart().getUser() == null
                        ? null
                        : item.getCart().getUser().getId())
                .orElseThrow(() -> new RuntimeException("Cart item not found"));
        currentUser.requireSelfOrAdmin(ownerId);
    }
}
