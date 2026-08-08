package com.app.fooddelivery.repository;

import com.app.fooddelivery.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
