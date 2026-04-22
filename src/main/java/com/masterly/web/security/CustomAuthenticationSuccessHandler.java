package com.masterly.web.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Обработчик успешной аутентификации.
 * Перенаправляет пользователя на его дашборд в зависимости от роли.
 */
@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * Перенаправляет пользователя после успешного входа.
     *
     * @param request запрос
     * @param response ответ
     * @param authentication данные аутентификации
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String redirectUrl = "/";

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            redirectUrl = "/admin/dashboard";
            log.debug("Admin logged in, redirecting to: {}", redirectUrl);
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MASTER"))) {
            redirectUrl = "/master/dashboard";
            log.debug("Master logged in, redirecting to: {}", redirectUrl);
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CLIENT"))) {
            redirectUrl = "/client/dashboard";
            log.debug("Client logged in, redirecting to: {}", redirectUrl);
        }

        response.sendRedirect(redirectUrl);
    }
}