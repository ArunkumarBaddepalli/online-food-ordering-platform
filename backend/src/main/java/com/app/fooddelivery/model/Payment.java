package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime paymentDate;
    private Double amount;
    private String paymentMethod; // COD or ONLINE
    private String paymentStatus; // PENDING, PAID, FAILED, CANCELLED

    // Razorpay references, kept for the receipt and for support queries.
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;
}
