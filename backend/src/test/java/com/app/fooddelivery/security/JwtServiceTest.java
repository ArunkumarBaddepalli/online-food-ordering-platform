package com.app.fooddelivery.security;

import com.app.fooddelivery.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "a-test-secret-long-enough-for-hmac-sha-signing-1234567890";

    private final JwtService service = new JwtService(SECRET, 24);

    private User user(Long id, String email, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setName("Test");
        return u;
    }

    @Test
    @DisplayName("A token carries the account it was issued for")
    void tokenCarriesIdentity() {
        Claims claims = service.parse(service.generateToken(user(7L, "a@test.com", "USER")));

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("a@test.com");
        assertThat(claims.get("userId", Integer.class)).isEqualTo(7);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("A token signed with a different secret is rejected")
    void tokenFromAnotherServerIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-value-0987654321-abcdef", 24);
        String foreign = other.generateToken(user(7L, "a@test.com", "ADMIN"));

        assertThat(service.parse(foreign))
                .as("only tokens this server signed may be trusted")
                .isNull();
    }

    @Test
    @DisplayName("An altered signature is rejected")
    void tamperedTokenIsRejected() {
        String token = service.generateToken(user(7L, "a@test.com", "USER"));

        // Changing signature characters, rather than appending. A single extra
        // character decodes to the same bytes, so it is not a real alteration.
        assertThat(service.parse(token.substring(0, token.length() - 4) + "AAAA")).isNull();
        assertThat(service.parse(token + "xxxx")).isNull();
        assertThat(service.parse(token.replace('.', ','))).isNull();
    }

    @Test
    @DisplayName("Rubbish and empty input are rejected rather than throwing")
    void garbageIsRejected() {
        assertThat(service.parse("")).isNull();
        assertThat(service.parse("not-a-token")).isNull();
        assertThat(service.parse("a.b.c")).isNull();
    }

    @Test
    @DisplayName("An expired token is rejected")
    void expiredTokenIsRejected() throws Exception {
        // Zero hours means the token expires the moment it is issued.
        JwtService shortLived = new JwtService(SECRET, 0);
        String token = shortLived.generateToken(user(7L, "a@test.com", "USER"));
        Thread.sleep(1100);

        assertThat(shortLived.parse(token)).isNull();
    }

    @Test
    @DisplayName("Editing the role inside a token invalidates it")
    void roleCannotBeEditedInsideAToken() {
        String token = service.generateToken(user(7L, "a@test.com", "USER"));
        String[] parts = token.split("\\.");

        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"a@test.com\",\"userId\":7,\"role\":\"ADMIN\"}".getBytes());

        assertThat(service.parse(parts[0] + "." + tamperedPayload + "." + parts[2]))
                .as("the signature no longer matches the edited payload")
                .isNull();
    }
}
