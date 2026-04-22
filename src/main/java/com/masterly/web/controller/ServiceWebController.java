package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.ServiceEntityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер управления услугами.
 */
@Slf4j
@Controller
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceWebController {

    private final CoreServiceClient coreServiceClient;

    private Long getMasterId(Authentication authentication) {
        if (authentication == null) return 1L;

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
     * Список услуг.
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
    public String listServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            Model model) {

        log.debug("Listing services - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<ServiceEntityDto> servicePage;

        if (isAdmin) {
            log.info("Admin viewing all services");
            servicePage = coreServiceClient.getAllServicesForAdmin(page, size, sortBy, sortDir);
        } else {
            Long masterId = getMasterId(authentication);
            log.debug("Master {} viewing services", masterId);
            servicePage = coreServiceClient.getServicesPaginated(page, size, sortBy, sortDir, masterId);
        }

        log.debug("Found {} services total", servicePage.getTotalElements());

        model.addAttribute("services", servicePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("totalItems", servicePage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "services/list";
    }
}