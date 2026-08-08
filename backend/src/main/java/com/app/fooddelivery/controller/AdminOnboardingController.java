package com.app.fooddelivery.controller;

import com.app.fooddelivery.dto.RejectRequest;
import com.app.fooddelivery.model.RestaurantOnboarding;
import com.app.fooddelivery.service.RestaurantOnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/onboarding")
public class AdminOnboardingController {

    @Autowired
    private RestaurantOnboardingService onboardingService;

    @GetMapping
    public ResponseEntity<List<RestaurantOnboarding>> listAll() {
        return ResponseEntity.ok(onboardingService.adminListAll());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(onboardingService.adminApprove(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody RejectRequest req) {
        try {
            return ResponseEntity.ok(onboardingService.adminReject(id, req.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/request-documents")
    public ResponseEntity<?> requestDocuments(@PathVariable Long id, @RequestBody RejectRequest req) {
        try {
            return ResponseEntity.ok(onboardingService.adminRequestDocuments(id, req.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
