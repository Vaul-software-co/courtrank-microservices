package com.courtrank.userService.infrastructure.config;

import com.courtrank.userService.application.ports.security.TokenService;
import com.courtrank.userService.application.ports.security.AuthSessionVerifier;
import com.courtrank.userService.infrastructure.security.InternalApiKeyFilter;
import com.courtrank.userService.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class HttpSecurityConfig {
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(TokenService tokenService, AuthSessionVerifier authSessionVerifier) {
        return new JwtAuthenticationFilter(tokenService, authSessionVerifier);
    }

    @Bean
    public InternalApiKeyFilter internalApiKeyFilter(
            @Value("${app.internal-api-key}") String internalApiKey
    ) {
        return new InternalApiKeyFilter(internalApiKey);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(this.csv(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "x-api-key",
                "x-sub-token",
                "x-club-id",
                "x-internal-api-key",
                "x-request-id"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalApiKeyFilter internalApiKeyFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
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
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/admin").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/me/privacy").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/me/lang").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/me/lang").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/me/avatar").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/users/me/avatar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/user/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/user/me/privacy").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/user/me/lang").authenticated()
                        .requestMatchers(HttpMethod.POST, "/user/me/lang").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/user/me/avatar").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/user/me/avatar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/user/**").authenticated()
                        .anyRequest().denyAll()
                )
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
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
