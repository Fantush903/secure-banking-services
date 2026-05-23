package com.banking.OnlineBankingWeb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/logout",
                                "/forgot-password", "/verify-otp",
                                "/reset-password", "/resend-otp",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }
}