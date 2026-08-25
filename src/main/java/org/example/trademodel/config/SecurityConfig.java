package org.example.trademodel.config;

import java.util.LinkedHashMap;

import org.example.trademodel.security.AuthAuditAuthenticationEntryPoint;
import org.example.trademodel.security.ActiveUserSessionFilter;
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
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${trade-model.auth.enabled:true}") boolean authEnabled,
                                            AuthAuditAuthenticationEntryPoint authAuditAuthenticationEntryPoint,
                                            PersonalLogoutSuccessHandler personalLogoutSuccessHandler,
                                            ActiveUserSessionFilter activeUserSessionFilter)
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
                .addFilterAfter(activeUserSessionFilter, SecurityContextHolderFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/login", "/register", "/owner/password-setup", "/favicon.ico", "/error", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                        .requestMatchers("/me/accounts", "/api/owner/**",
                                "/api/review/rule-version-logs").hasRole("OWNER")
                        .requestMatchers("/api/dashboard/home", "/api/dashboard/trace-summary").authenticated()
                        .requestMatchers("/api/dashboard/**", "/api/settings/notifications/telegram/**").hasRole("OWNER")
                        .requestMatchers("/api/provider-call/base-profile", "/api/config/scan-profile", "/api/config/scan-profile/**",
                                "/api/ai/orchestrator/status", "/api/ai/providers/**",
                                "/api/push/recheck/dispatch/**", "/api/push/recheck/replay/**",
                                "/api/push/recheck/replay", "/api/push/recheck/ops/**",
                                "/api/rule/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/external-context/macro-events/import",
                                "/api/external-context/news-events/import").hasRole("OWNER")
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
