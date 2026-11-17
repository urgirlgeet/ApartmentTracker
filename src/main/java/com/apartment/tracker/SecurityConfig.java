package com.apartment.tracker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/signup", "/css/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/notices").hasRole("ADMIN")
                .requestMatchers("/profile", "/profile/**").authenticated()
                .requestMatchers("/maintenance/staff/**").hasRole("MAINTENANCE_HEAD")
                .requestMatchers(HttpMethod.POST, "/complaints/*/advance", "/notices/*/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/complaints/*/confirm", "/complaints/*/reopen").hasRole("RESIDENT")
                .requestMatchers(HttpMethod.POST, "/complaints/*/assign").hasAnyRole("ADMIN", "MAINTENANCE_HEAD")
                .requestMatchers(HttpMethod.POST, "/complaints/*/status").hasAnyRole("ADMIN", "MAINTENANCE_HEAD")
                .requestMatchers(HttpMethod.POST, "/bookings").hasRole("RESIDENT")
                .requestMatchers("/bookings/**").hasAnyRole("RESIDENT", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/bills").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/bills/*/payments").hasRole("RESIDENT")
                .requestMatchers("/bills/**").hasAnyRole("RESIDENT", "ADMIN")
                .requestMatchers("/complaints/**", "/notices/**", "/dashboard").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
