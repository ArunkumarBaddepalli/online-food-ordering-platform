package com.app.fooddelivery.controller;

import com.app.fooddelivery.dto.BankDetailsRequest;
import com.app.fooddelivery.dto.BasicInfoRequest;
import com.app.fooddelivery.dto.HoursWrapperRequest;
import com.app.fooddelivery.dto.LocationRequest;
import com.app.fooddelivery.model.RestaurantOnboarding;
import com.app.fooddelivery.service.RestaurantOnboardingService;
import com.app.fooddelivery.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    @Autowired
    private RestaurantOnboardingService onboardingService;

    @Autowired
    private CurrentUser currentUser;

    @PostMapping("/start")
    public ResponseEntity<?> startOnboarding(@RequestParam Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        try {
            RestaurantOnboarding ob = onboardingService.getOrStartOnboarding(userId);
            return ResponseEntity.ok(ob);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOnboarding(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(onboardingService.getOnboarding(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getOnboardingStatus(@PathVariable Long userId) {
        currentUser.requireSelfOrAdmin(userId);
        try {
            return ResponseEntity.ok(onboardingService.getOnboardingByUserId(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/step/basic-info")
    public ResponseEntity<?> saveBasicInfo(@PathVariable Long id, @RequestBody BasicInfoRequest req) {
        requireOwnApplication(id);
        try {
            return ResponseEntity.ok(onboardingService.saveBasicInfo(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/step/location")
    public ResponseEntity<?> saveLocation(@PathVariable Long id, @RequestBody LocationRequest req) {
        requireOwnApplication(id);
        try {
            return ResponseEntity.ok(onboardingService.saveLocation(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/step/hours")
    public ResponseEntity<?> saveHours(@PathVariable Long id, @RequestBody HoursWrapperRequest req) {
        requireOwnApplication(id);
        try {
            return ResponseEntity.ok(onboardingService.saveOperatingHours(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/step/documents")
    public ResponseEntity<?> saveDocuments(
            @PathVariable Long id,
            @RequestParam String fssaiLicenseNumber,
            @RequestParam String panNumber,
            @RequestParam(required = false) String gstin,
            @RequestParam(required = false) MultipartFile fssaiDocument) {
        try {
            return ResponseEntity.ok(onboardingService.saveDocuments(id, fssaiLicenseNumber, panNumber, gstin, fssaiDocument));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/step/bank-details")
    public ResponseEntity<?> saveBankDetails(@PathVariable Long id, @RequestBody BankDetailsRequest req) {
        requireOwnApplication(id);
        try {
            return ResponseEntity.ok(onboardingService.saveBankDetails(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitOnboarding(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(onboardingService.submitOnboarding(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** An application can only be read or edited by the person who started it. */
    private void requireOwnApplication(Long onboardingId) {
        Long applicantId = onboardingService.getOnboarding(onboardingId).getUserId();
        currentUser.requireSelfOrAdmin(applicantId);
    }
}
