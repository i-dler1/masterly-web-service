package com.masterly.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Контроллер аутентификации.
 */
@Slf4j
@Controller
public class LoginController {

    /**
     * Страница входа.
     *
     * @return название шаблона
     */
    @GetMapping("/login")
    public String login() {
        log.debug("Showing login page");
        return "login";
    }

    /**
     * Главная страница после входа.
     * Перенаправляет на дашборд в зависимости от роли.
     *
     * @param authentication данные аутентификации
     * @return редирект на дашборд или главную страницу
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isMaster = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER"));
        boolean isClient = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isMaster) {
            return "redirect:/master/dashboard";
        } else if (isClient) {
            return "redirect:/clients/dashboard";
        }
        return "index";
    }

    /**
     * Страница отказа в доступе.
     *
     * @return название шаблона
     */
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}