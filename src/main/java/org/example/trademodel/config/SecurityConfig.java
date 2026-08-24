package org.example.trademodel.config;

import java.util.LinkedHashMap;

import org.example.trademodel.security.AuthAuditAuthenticationEntryPoint;
import org.example.trademodel.security.PersonalLogoutSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${trade-model.auth.enabled:true}") boolean authEnabled,
                                            AuthAuditAuthenticationEntryPoint authAuditAuthenticationEntryPoint,
                                            PersonalLogoutSuccessHandler personalLogoutSuccessHandler)
            throws Exception {
        if (!authEnabled) {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .build();
        }

        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(new AntPathRequestMatcher("/api/**"), authAuditAuthenticationEntryPoint);
        DelegatingAuthenticationEntryPoint authenticationEntryPoint =
                new DelegatingAuthenticationEntryPoint(entryPoints);
        authenticationEntryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));

        return http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", false)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(personalLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/login", "/favicon.ico", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
