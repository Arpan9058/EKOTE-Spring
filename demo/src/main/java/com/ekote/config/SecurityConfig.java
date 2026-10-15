package com.ekote.config;

import com.ekote.service.CustomUserDetailsService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;

  public SecurityConfig(CustomUserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/",
                        "/login",
                        "/register",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/img/**",
                        "/css/**",
                        "/js/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/inventory/**",
                        "/recordBook/**",
                        "/gunDetails/**",
                        "/actuator/**",
                        "/dashboard",
                        "/guns/**",
                        "/gunForm/**",
                        "/issue/**",
                        "/return",
                        "/updateMaintenance")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login") // your login.html
                    .defaultSuccessUrl("/", false)
                    .failureUrl("/login?error=invalid_credentials")
                    .permitAll())
        .logout(
            logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());

    return http.build();
  }
}
