package com.app.fooddelivery.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String role; // USER, ADMIN, RESTAURANT_OWNER

    private String address;

    // Establishing relationship with Cart if needed, or Cart can verify User
    // For simplicity, we might just link via ID or one-to-one
}
