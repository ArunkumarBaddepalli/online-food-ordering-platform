package com.app.fooddelivery.controller;

import com.app.fooddelivery.service.RazorpayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * What this deployment can actually do, so the app offers only what works.
 *
 * The alternative is hiding things in the interface while the API still accepts
 * them, which is a lie that holds right up until somebody calls it directly.
 */
@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    @Autowired
    private RazorpayPaymentService razorpayService;

    @Value("${orders.delivery-enabled:true}")
    private boolean deliveryEnabled;

    @GetMapping
    public ResponseEntity<Map<String, Object>> config() {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryEnabled", deliveryEnabled);
        body.put("onlinePaymentEnabled", razorpayService.isEnabled());
        body.put("razorpayKeyId", razorpayService.getKeyId() == null ? "" : razorpayService.getKeyId());
        return ResponseEntity.ok(body);
    }
}
