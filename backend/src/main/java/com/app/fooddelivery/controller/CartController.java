package com.app.fooddelivery.controller;

import com.app.fooddelivery.dto.CartValidationResponse;
import com.app.fooddelivery.model.Cart;
import com.app.fooddelivery.model.CartItem;
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

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long userId, @RequestParam Long foodId,
            @RequestParam int quantity, @RequestParam(required = false) List<Long> modifierIds) {
        try {
            return ResponseEntity.ok(cartService.addToCart(userId, foodId, quantity, modifierIds));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<CartItem> updateCartItem(@PathVariable Long itemId, @RequestParam int quantity) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (quantity > item.getFoodItem().getStockQuantity()) {
            throw new RuntimeException(
                    "Cannot update quantity. Only " + item.getFoodItem().getStockQuantity() + " available.");
        }

        item.setQuantity(quantity);
        item.setTotalPrice(item.getFoodItem().getPrice() * quantity);
        return ResponseEntity.ok(cartItemRepository.save(item));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<String> removeCartItem(@PathVariable Long itemId) {
        cartItemRepository.deleteById(itemId);
        return ResponseEntity.ok("Item removed from cart");
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<String> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }

    @PostMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestParam Long userId, @RequestParam Long orderId) {
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
        return ResponseEntity.ok(cartService.validateCart(userId));
    }
}
