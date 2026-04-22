package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.MasterUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер профиля пользователя.
 */
@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CoreServiceClient coreServiceClient;

    /**
     * Просмотр профиля.
     *
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона или "error" при ошибке
     */
    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        String email = authentication.getName();
        log.debug("Showing profile for user: {}", email);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);
            model.addAttribute("master", master);

            if (isAdmin) {
                return "admin/profile";
            } else {
                return "masters/profile";
            }
        } catch (Exception e) {
            log.error("Error loading profile: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Форма редактирования профиля.
     *
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона или "error" при ошибке
     */
    @GetMapping("/edit")
    public String showEditForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        log.debug("Showing edit form for user: {}", email);

        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);

            MasterUpdateDto updateDto = new MasterUpdateDto();
            updateDto.setFullName(master.getFullName());
            updateDto.setPhone(master.getPhone());
            updateDto.setBusinessName(master.getBusinessName());
            updateDto.setSpecialization(master.getSpecialization());

            model.addAttribute("master", updateDto);
            return "profile/edit";
        } catch (Exception e) {
            log.error("Error loading edit form: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Обновление профиля.
     *
     * @param updateDto данные для обновления
     * @param authentication данные аутентификации
     * @return редирект на профиль
     */
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute MasterUpdateDto updateDto, Authentication authentication) {
        String email = authentication.getName();
        log.info("Updating profile for user: {}", email);

        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);
            coreServiceClient.updateMasterProfile(master.getId(), updateDto);
            log.info("Profile updated successfully");
            return "redirect:/profile";
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            return "redirect:/profile?error";
        }
    }
}