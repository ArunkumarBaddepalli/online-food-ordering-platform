package com.app.fooddelivery.controller;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.model.UserAddress;
import com.app.fooddelivery.repository.UserAddressRepository;
import com.app.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAddressController {

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.app.fooddelivery.service.GeocodingService geocodingService;

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<UserAddress>> getUserAddresses(@PathVariable Long userId) {
        return ResponseEntity.ok(userAddressRepository.findByUserId(userId));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<UserAddress> addUserAddress(@PathVariable Long userId, @RequestBody UserAddress address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateAndGeocodeAddress(address);

        address.setUser(user);
        return ResponseEntity.ok(userAddressRepository.save(address));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<String> deleteUserAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        userAddressRepository.deleteById(addressId);
        return ResponseEntity.ok("Address deleted");
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<UserAddress> updateUserAddress(@PathVariable Long userId, @PathVariable Long addressId,
            @RequestBody UserAddress addressDetails) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to address");
        }

        address.setAddressLine(addressDetails.getAddressLine());
        address.setLabel(addressDetails.getLabel());
        address.setPincode(addressDetails.getPincode());

        validateAndGeocodeAddress(address);

        return ResponseEntity.ok(userAddressRepository.save(address));
    }

    private void validateAndGeocodeAddress(UserAddress address) {
        if (address.getPincode() == null || address.getPincode().trim().isEmpty()) {
            throw new RuntimeException("Pincode is mandatory");
        }

        String fullAddress = address.getAddressLine() + ", " + address.getPincode();
        var result = geocodingService.geocodeAddress(fullAddress);

        if (result == null) {
            throw new RuntimeException("Invalid address or pincode. Could not locate on map.");
        }

        address.setLatitude(result.getLatitude());
        address.setLongitude(result.getLongitude());
    }
}
