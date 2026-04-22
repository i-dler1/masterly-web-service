package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.MaterialDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер управления материалами.
 */
@Slf4j
@Controller
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialWebController {

    private final CoreServiceClient coreServiceClient;

    private Long getMasterId(Authentication authentication) {
        String email = authentication.getName();
        log.debug("Getting master ID for email: {}", email);

        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);
            return master.getId();
        } catch (Exception e) {
            log.error("Error getting master ID: {}", e.getMessage());
            return 1L;
        }
    }

    /**
     * Список материалов.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping
    public String listMaterials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            Model model) {

        log.debug("Listing materials - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<MaterialDto> materialPage;

        if (isAdmin) {
            log.info("Admin viewing all materials");
            materialPage = coreServiceClient.getAllMaterialsForAdmin(page, size, sortBy, sortDir);
        } else {
            Long masterId = getMasterId(authentication);
            log.debug("Master {} viewing materials", masterId);
            materialPage = coreServiceClient.getMaterialsPaginated(page, size, sortBy, sortDir, masterId);
        }

        log.debug("Found {} materials total", materialPage.getTotalElements());

        model.addAttribute("materials", materialPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", materialPage.getTotalPages());
        model.addAttribute("totalItems", materialPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "materials/list";
    }

    /**
     * Форма создания материала.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.debug("Showing create material form");
        model.addAttribute("material", new MaterialDto());
        return "materials/form";
    }

    /**
     * Форма редактирования материала.
     *
     * @param id идентификатор материала
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Authentication authentication, Model model) {
        log.debug("Showing edit form for material: {}", id);

        Long masterId = getMasterId(authentication);
        MaterialDto material = coreServiceClient.getMaterial(id, masterId);

        log.debug("Loaded material: {}", material.getName());
        model.addAttribute("material", material);
        return "materials/form";
    }

    /**
     * Сохранение материала.
     *
     * @param materialDto данные материала
     * @param authentication данные аутентификации
     * @return редирект на список материалов
     */
    @PostMapping("/save")
    public String saveMaterial(@ModelAttribute MaterialDto materialDto, Authentication authentication) {
        Long masterId = getMasterId(authentication);

        if (materialDto.getId() == null) {
            log.info("Creating new material: {}", materialDto.getName());
            coreServiceClient.createMaterial(masterId, materialDto);
            log.debug("Material created successfully");
        } else {
            log.info("Updating material: {}", materialDto.getId());
            coreServiceClient.updateMaterial(materialDto.getId(), masterId, materialDto);
            log.debug("Material {} updated successfully", materialDto.getId());
        }

        return "redirect:/materials";
    }

    /**
     * Удаление материала.
     *
     * @param id идентификатор материала
     * @param authentication данные аутентификации
     * @return редирект на список материалов
     */
    @GetMapping("/delete/{id}")
    public String deleteMaterial(@PathVariable Long id, Authentication authentication) {
        log.info("Deleting material: {}", id);

        Long masterId = getMasterId(authentication);

        try {
            coreServiceClient.deleteMaterial(id);
            log.debug("Material {} deleted successfully", id);
            return "redirect:/materials";
        } catch (Exception e) {
            log.error("Error deleting material: {}", e.getMessage());
            return "redirect:/materials?error";
        }
    }
}