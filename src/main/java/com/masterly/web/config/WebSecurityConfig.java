package com.masterly.web.config;

import com.masterly.web.security.CoreAuthenticationProvider;
import com.masterly.web.security.CustomAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности веб-приложения.
 * Настраивает доступы по ролям и форму логина.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CoreAuthenticationProvider coreAuthenticationProvider;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    /**
     * Настраивает цепочку фильтров безопасности.
     *
     * @param http конфигуратор HTTP безопасности
     * @return цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring web security");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(coreAuthenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/access-denied").permitAll()

                        // Админ
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Мастер
                        .requestMatchers("/master/**").hasRole("MASTER")

                        // Клиент
                        .requestMatchers("/client/**").hasRole("CLIENT")

                        // Общие страницы (доступны всем авторизованным)
                        .requestMatchers("/services", "/materials", "/clients", "/appointments", "/profile", "/masters").authenticated()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        log.info("Web security configuration completed");
        return http.build();
    }
}