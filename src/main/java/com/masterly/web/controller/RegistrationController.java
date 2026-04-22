package com.masterly.web.controller;

import com.masterly.web.client.CoreAuthClient;
import com.masterly.web.dto.MasterRegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Контроллер регистрации новых мастеров.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final CoreAuthClient coreAuthClient;

    /**
     * Форма регистрации.
     *
     * @return название шаблона
     */
    @GetMapping("/register")
    public String showRegistrationForm() {
        log.debug("Showing registration form");
        return "register";
    }

    /**
     * Обработка регистрации.
     *
     * @param email email мастера
     * @param password пароль
     * @param fullName полное имя
     * @param phone телефон
     * @param role роль
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу входа или возврат к форме при ошибке
     */
    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String fullName,
                           @RequestParam String phone,
                           @RequestParam String role,
                           RedirectAttributes redirectAttributes) {
        log.info("New registration attempt for email: {}, role: {}", email, role);

        try {
            MasterRegisterRequest request = MasterRegisterRequest.builder()
                    .email(email)
                    .password(password)
                    .fullName(fullName)
                    .phone(phone)
                    .role(role)
                    .build();

            coreAuthClient.register(request);
            redirectAttributes.addFlashAttribute("success", "Регистрация успешна! Войдите в систему.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Registration failed for email: {} - {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка регистрации. Email может быть уже занят.");
            return "redirect:/register";
        }
    }
}