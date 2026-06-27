package org.example.trademodel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${trade-model.auth.enabled:true}") boolean authEnabled) throws Exception {
        // V1 APIs are stateless JSON endpoints used with Basic Auth; CSRF tokens would break the current fetch/API clients.
        http.csrf(AbstractHttpConfigurer::disable);

        if (!authEnabled) {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
        }

        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/favicon.ico", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${trade-model.auth.admin-username:admin}") String username,
            @Value("${trade-model.auth.admin-password:dev-local-password}") String password,
            PasswordEncoder passwordEncoder) {
        String safeUsername = hasText(username) ? username.trim() : "admin";
        String safePassword = hasText(password) ? password : "dev-local-password";
        return new InMemoryUserDetailsManager(User.withUsername(safeUsername)
                .password(passwordEncoder.encode(safePassword))
                .roles("OPERATOR")
                .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
