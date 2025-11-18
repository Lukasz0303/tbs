package com.tbs.config;

import com.tbs.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Objects;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final List<String> exposedHeaders;
    private final long maxAge;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("#{'${app.cors.allowed-origins}'.split(',')}") List<String> allowedOrigins,
            @Value("#{'${app.cors.allowed-methods}'.split(',')}") List<String> allowedMethods,
            @Value("#{'${app.cors.allowed-headers}'.split(',')}") List<String> allowedHeaders,
            @Value("#{'${app.cors.exposed-headers}'.split(',')}") List<String> exposedHeaders,
            @Value("${app.cors.max-age:3600}") long maxAge
    ) {
        this.jwtAuthenticationFilter = Objects.requireNonNull(jwtAuthenticationFilter);
        
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-origins cannot be empty");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-methods cannot be empty");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            throw new IllegalArgumentException("app.cors.allowed-headers cannot be empty");
        }
        if (exposedHeaders == null || exposedHeaders.isEmpty()) {
            throw new IllegalArgumentException("app.cors.exposed-headers cannot be empty");
        }
        
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.maxAge = maxAge;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setExposedHeaders(exposedHeaders);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/guests").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/rankings/**").permitAll()
                        .requestMatchers("/api/websocket/**").permitAll()
                        .requestMatchers("/api/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/prometheus").authenticated()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/users/{userId}").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/users/{userId}/last-seen").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/users/{userId}").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

