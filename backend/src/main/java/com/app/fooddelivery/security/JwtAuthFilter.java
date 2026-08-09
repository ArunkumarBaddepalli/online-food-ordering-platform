package com.app.fooddelivery.security;

import com.app.fooddelivery.model.User;
import com.app.fooddelivery.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads the Authorization header on every request and, when it holds a valid
 * token, tells Spring Security who is calling.
 *
 * A missing or bad token is not an error here — the request simply stays
 * anonymous, and the rules in SecurityConfig decide whether that is allowed.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            Claims claims = jwtService.parse(header.substring(7));

            if (claims != null) {
                // Load the user rather than trusting the token's copy of the
                // role, so a role change takes effect without re-issuing.
                Optional<User> found = userRepository.findByEmail(claims.getSubject());

                if (found.isPresent()) {
                    User user = found.get();
                    String role = user.getRole() == null ? "USER" : user.getRole();

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(user.getId(), user.getEmail(), role),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Replace the whole context rather than mutating the
                    // deferred one, which is what Spring Security 6 expects.
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
