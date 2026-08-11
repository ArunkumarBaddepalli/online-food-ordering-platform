package com.app.fooddelivery.service;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Proving an address belongs to someone, and letting them back in when they
 * have forgotten their password.
 */
@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    private static final int VERIFICATION_HOURS = 48;
    private static final int RESET_MINUTES = 30;

    private final UserRepository userRepository;
    private final EmailService email;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AccountEmailService(UserRepository userRepository, EmailService email,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.email = email;
        this.passwordEncoder = passwordEncoder;
    }

    /** Unguessable, which matters: this token is the only thing protecting an account. */
    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // --- Verifying an address -----------------------------------------------

    @Transactional
    public void sendVerification(User user) {
        user.setVerificationToken(newToken());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(VERIFICATION_HOURS));
        userRepository.save(user);

        String link = email.baseUrl() + "/verify-email?token=" + user.getVerificationToken();

        email.send(user.getEmail(), "Confirm your email address",
                email.layout("Confirm your email address", """
                        <p>Hello %s,</p>
                        <p>Please confirm this address so we can reach you about your orders,
                           and so you can reset your password if you ever need to.</p>
                        %s
                        <p style="font-size:13px;color:#6c757d">This link works for %d hours.
                           If you did not create an account, you can ignore this message.</p>
                        """.formatted(
                        user.getName() == null ? "there" : user.getName(),
                        email.button(link, "Confirm my email"),
                        VERIFICATION_HOURS)));
    }

    @Transactional
    public boolean verify(String token) {
        Optional<User> found = userRepository.findByVerificationToken(token);
        if (found.isEmpty()) {
            return false;
        }

        User user = found.get();
        if (user.getVerificationTokenExpiry() == null
                || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setEmailVerified(true);
        // Single use: the link cannot be replayed once it has done its job.
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for {}", user.getEmail());
        return true;
    }

    @Transactional
    public void resendVerification(String emailAddress) {
        userRepository.findByEmail(emailAddress)
                .filter(u -> !Boolean.TRUE.equals(u.getEmailVerified()))
                .ifPresent(this::sendVerification);
    }

    // --- Forgotten passwords ------------------------------------------------

    /**
     * Always appears to succeed, whether or not the address is registered.
     * Saying "no such account" would let anybody test which addresses exist.
     */
    @Transactional
    public void requestPasswordReset(String emailAddress) {
        Optional<User> found = userRepository.findByEmail(emailAddress);
        if (found.isEmpty()) {
            log.info("Password reset asked for an address with no account: {}", emailAddress);
            return;
        }

        User user = found.get();
        user.setResetToken(newToken());
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_MINUTES));
        userRepository.save(user);

        String link = email.baseUrl() + "/reset-password?token=" + user.getResetToken();

        email.send(user.getEmail(), "Reset your password",
                email.layout("Reset your password", """
                        <p>Hello %s,</p>
                        <p>Somebody asked to reset the password for this account.</p>
                        %s
                        <p style="font-size:13px;color:#6c757d">This link works for %d minutes and
                           can be used once. If this was not you, nothing has changed and you can
                           ignore this message.</p>
                        """.formatted(
                        user.getName() == null ? "there" : user.getName(),
                        email.button(link, "Choose a new password"),
                        RESET_MINUTES)));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 8) {
            throw new RuntimeException("Choose a password of at least 8 characters.");
        }

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("That reset link is not valid."));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("That reset link has expired. Please ask for a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        // Whoever reset it proved they can read the inbox, so the address is
        // real. Verifying it here saves a pointless second round trip.
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Password reset for {}", user.getEmail());
    }
}
