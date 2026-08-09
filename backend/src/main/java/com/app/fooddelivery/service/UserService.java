package com.app.fooddelivery.service;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("USER");
        }
        return userRepository.save(user);
    }

    public User loginUser(String email, String password) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || password == null) {
            return null;
        }

        User user = found.get();
        String stored = user.getPassword();
        if (stored == null) {
            return null;
        }

        if (passwordEncoder.matches(password, stored)) {
            return user;
        }

        // Accounts created before hashing was added still hold a plaintext
        // password. Accept it once, then upgrade it to a hash in place.
        if (!isHashed(stored) && stored.equals(password)) {
            user.setPassword(passwordEncoder.encode(password));
            return userRepository.save(user);
        }

        return null;
    }

    private boolean isHashed(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
