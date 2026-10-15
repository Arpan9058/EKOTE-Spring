package com.ekote.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ManagementSecurityConfig {
  @Bean
  @Order(1)
  SecurityFilterChain internalManagement(
      HttpSecurity http, @Value("${management.server.port:-1}") int port) throws Exception {
    // Only a separately configured, network-isolated management listener is available to scrapers.
    http.securityMatcher(request -> port > 0 && request.getLocalPort() == port)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus")
                    .permitAll()
                    .anyRequest()
                    .denyAll());
    return http.build();
  }
}
