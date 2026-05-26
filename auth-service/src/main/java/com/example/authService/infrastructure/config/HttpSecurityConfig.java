package com.example.authService.infrastructure.config;

import com.example.authService.application.ports.security.ClientVerifier;
import com.example.authService.application.ports.security.TokenService;
import com.example.authService.domain.repository.SessionRepository;
import com.example.authService.infrastructure.security.ApiKeyFilter;
import com.example.authService.infrastructure.security.InMemoryRateLimitFilter;
import com.example.authService.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.time.Clock;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CookieProperties.class)
public class HttpSecurityConfig {
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService, SessionRepository sessionRepository) {
        return new JwtAuthenticationFilter(tokenService, sessionRepository);
    }

    @Bean
    public ApiKeyFilter apiKeyFilter(ClientVerifier clientVerifier) {
        return new ApiKeyFilter(clientVerifier);
    }

    @Bean
    public InMemoryRateLimitFilter inMemoryRateLimitFilter() {
        return new InMemoryRateLimitFilter(Clock.systemUTC());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(this.csv(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "x-api-key", "x-request-id"));
        config.setExposedHeaders(List.of("Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InMemoryRateLimitFilter rateLimitFilter,
            ApiKeyFilter apiKeyFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-email/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/resend-verification-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-email/resend").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/request-password-reset").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/verify-password-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password-reset/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/auth/password-reset/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/change-password").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/auth/sessions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/auth/sessions").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/auth/sessions/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private List<String> csv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
