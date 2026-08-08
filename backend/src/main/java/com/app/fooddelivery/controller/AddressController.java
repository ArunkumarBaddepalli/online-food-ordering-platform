package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.SavedAddress;
import com.app.fooddelivery.repository.SavedAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing user saved addresses.
 */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private SavedAddressRepository savedAddressRepository;

    /**
     * Get all saved addresses for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SavedAddress>> getUserAddresses(@PathVariable Long userId) {
        List<SavedAddress> addresses = savedAddressRepository.findByUserId(userId);
        return ResponseEntity.ok(addresses);
    }

    /**
     * Get user's default address
     */
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<SavedAddress> getDefaultAddress(@PathVariable Long userId) {
        return savedAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new saved address
     */
    @PostMapping
    public ResponseEntity<SavedAddress> createAddress(@RequestBody SavedAddress address) {
        // If this is marked as default, unset other default addresses for this user
        if (address.getIsDefault() != null && address.getIsDefault()) {
            List<SavedAddress> userAddresses = savedAddressRepository.findByUserId(address.getUser().getId());
            userAddresses.forEach(addr -> {
                addr.setIsDefault(false);
                savedAddressRepository.save(addr);
            });
        }

        SavedAddress saved = savedAddressRepository.save(address);
        return ResponseEntity.ok(saved);
    }

    /**
     * Update an existing address
     */
    @PutMapping("/{id}")
    public ResponseEntity<SavedAddress> updateAddress(
            @PathVariable Long id,
            @RequestBody SavedAddress address) {
        return savedAddressRepository.findById(id)
                .map(existing -> {
                    existing.setLabel(address.getLabel());
                    existing.setStreet(address.getStreet());
                    existing.setCity(address.getCity());
                    existing.setState(address.getState());
                    existing.setZipCode(address.getZipCode());
                    existing.setCountry(address.getCountry());
                    existing.setLatitude(address.getLatitude());
                    existing.setLongitude(address.getLongitude());
                    existing.setAdditionalInstructions(address.getAdditionalInstructions());

                    // Handle default address change
                    if (address.getIsDefault() != null && address.getIsDefault()) {
                        List<SavedAddress> userAddresses = savedAddressRepository
                                .findByUserId(existing.getUser().getId());
                        userAddresses.forEach(addr -> {
                            addr.setIsDefault(false);
                            savedAddressRepository.save(addr);
                        });
                        existing.setIsDefault(true);
                    }

                    SavedAddress updated = savedAddressRepository.save(existing);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an address
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        if (savedAddressRepository.existsById(id)) {
            savedAddressRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Set an address as default
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<SavedAddress> setDefaultAddress(@PathVariable Long id) {
        return savedAddressRepository.findById(id)
                .map(address -> {
                    // Unset other default addresses
                    List<SavedAddress> userAddresses = savedAddressRepository.findByUserId(address.getUser().getId());
                    userAddresses.forEach(addr -> {
                        addr.setIsDefault(false);
                        savedAddressRepository.save(addr);
                    });

                    // Set this as default
                    address.setIsDefault(true);
                    SavedAddress updated = savedAddressRepository.save(address);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
