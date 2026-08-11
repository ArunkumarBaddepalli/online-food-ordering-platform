package com.app.fooddelivery.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

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

    // WRITE_ONLY: accepted on register/login, never serialised back to a client.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Whether the address has been proved to exist and belong to this person.
    // Nothing else can prove it: a well-formed address at a real domain that
    // nobody owns looks identical to a good one until a message is sent.
    private Boolean emailVerified = false;

    @JsonIgnore
    private String verificationToken;
    @JsonIgnore
    private LocalDateTime verificationTokenExpiry;

    @JsonIgnore
    private String resetToken;
    @JsonIgnore
    private LocalDateTime resetTokenExpiry;

    private String role; // USER, ADMIN, RESTAURANT_OWNER

    private String address;

    // Establishing relationship with Cart if needed, or Cart can verify User
    // For simplicity, we might just link via ID or one-to-one
}
