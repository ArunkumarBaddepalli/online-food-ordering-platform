package com.app.fooddelivery.service;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Password handling regression tests. Uses a real BCrypt encoder rather than a
 * mock, so the assertions cover the actual hashing behaviour.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    private User newUser(String email, String password) {
        User u = new User();
        u.setName("Test");
        u.setEmail(email);
        u.setPassword(password);
        return u;
    }

    @Test
    @DisplayName("Registration stores a BCrypt hash, never the plain password")
    void registrationHashesPassword() {
        User saved = userService.registerUser(newUser("a@test.com", "secret123"));

        assertThat(saved.getPassword()).isNotEqualTo("secret123");
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(encoder.matches("secret123", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Registration defaults the role to USER")
    void registrationDefaultsRole() {
        assertThat(userService.registerUser(newUser("a@test.com", "secret123")).getRole())
                .isEqualTo("USER");
    }

    @Test
    @DisplayName("A duplicate email is rejected with a clear message")
    void duplicateEmailRejected() {
        when(userRepository.findByEmail("taken@test.com"))
                .thenReturn(Optional.of(newUser("taken@test.com", "whatever")));

        assertThatThrownBy(() -> userService.registerUser(newUser("taken@test.com", "secret123")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("A blank password is rejected")
    void blankPasswordRejected() {
        assertThatThrownBy(() -> userService.registerUser(newUser("a@test.com", "  ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    @DisplayName("Login succeeds with the correct password and fails with the wrong one")
    void loginChecksHashedPassword() {
        User stored = newUser("a@test.com", encoder.encode("secret123"));
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(stored));

        assertThat(userService.loginUser("a@test.com", "secret123")).isNotNull();
        assertThat(userService.loginUser("a@test.com", "wrong")).isNull();
    }

    @Test
    @DisplayName("A legacy plaintext account logs in once, then is upgraded to a hash")
    void legacyPlaintextPasswordIsUpgraded() {
        User legacy = newUser("old@test.com", "password");
        when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(legacy));

        User loggedIn = userService.loginUser("old@test.com", "password");

        assertThat(loggedIn).isNotNull();
        assertThat(legacy.getPassword())
                .as("the plaintext row must be rewritten as a hash")
                .startsWith("$2a$");
        assertThat(encoder.matches("password", legacy.getPassword())).isTrue();
    }

    @Test
    @DisplayName("A wrong password against a legacy plaintext account still fails")
    void legacyPlaintextWrongPasswordFails() {
        User legacy = newUser("old@test.com", "password");
        when(userRepository.findByEmail("old@test.com")).thenReturn(Optional.of(legacy));

        assertThat(userService.loginUser("old@test.com", "guess")).isNull();
        assertThat(legacy.getPassword()).isEqualTo("password");
    }

    @Test
    @DisplayName("Login with an unknown email returns null")
    void unknownEmailFails() {
        assertThat(userService.loginUser("nobody@test.com", "whatever")).isNull();
    }
}
