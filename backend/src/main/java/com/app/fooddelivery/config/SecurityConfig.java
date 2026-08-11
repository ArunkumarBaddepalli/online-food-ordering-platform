package com.app.fooddelivery.config;

import com.app.fooddelivery.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // No server-side session: every request proves itself with a token,
                // so CSRF protection is not the relevant defence here.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Signing in and signing up cannot require a token.
                        .requestMatchers("/api/auth/**").permitAll()

                        // Browsing restaurants and menus is open to visitors, the
                        // same as any food app's home page.
                        .requestMatchers(HttpMethod.GET,
                                "/api/restaurants",
                                "/api/restaurants/*",
                                "/api/restaurants/*/live-status",
                                "/api/restaurants/*/menu-status",
                                "/api/restaurants/*/time-slots",
                                "/api/restaurants/*/reviews",
                                "/api/restaurants/ratings",
                                "/api/restaurants/cuisines",
                                "/api/foods/*",
                                "/api/modifiers/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/*/validate-delivery").permitAll()
                        .requestMatchers("/api/payment/config", "/api/config").permitAll()

                        // API documentation and uploaded images.
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**",
                                "/swagger-ui.html", "/uploads/**")
                        .permitAll()

                        // Moderation is admin only.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Menu and restaurant configuration belong to the owner of
                        // that restaurant; the controllers check which one.
                        .requestMatchers("/api/menu/**").hasAnyRole("RESTAURANT_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/restaurants/*/settings",
                                "/api/restaurants/*/auto-accept")
                        .hasAnyRole("RESTAURANT_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/restaurants").hasRole("ADMIN")

                        // Everything else needs a signed-in caller.
                        .anyRequest().authenticated())

                // Written directly rather than via sendError: sendError triggers a
                // container ERROR dispatch that re-enters this filter chain with an
                // empty context, and the resulting 401 overwrites the real 403.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writePlain(res, 401, "Sign in required"))
                        .accessDeniedHandler((req, res, ex) -> writePlain(res, 403, "Not allowed")))

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writePlain(jakarta.servlet.http.HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
