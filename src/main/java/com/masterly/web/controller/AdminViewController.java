package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер панели администратора.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final CoreServiceClient coreServiceClient;

    /**
     * Главная страница админ-панели.
     *
     * @return название шаблона
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    /**
     * Страница управления услугами.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/services")
    public String services(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<ServiceEntityDto> services = coreServiceClient.getAllServicesForAdmin(page, size, "name", "asc");
        model.addAttribute("services", services.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", services.getTotalPages());
        model.addAttribute("size", size);
        return "admin/services";
    }

    /**
     * Страница управления материалами.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/materials")
    public String materials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<MaterialDto> materials = coreServiceClient.getAllMaterialsForAdmin(page, size, "name", "asc");
        model.addAttribute("materials", materials.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", materials.getTotalPages());
        model.addAttribute("size", size);
        return "admin/materials";
    }

    /**
     * Страница управления клиентами.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/clients")
    public String clients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<ClientDto> clients = coreServiceClient.getAllClientsForAdmin(page, size, "fullName", "asc");
        model.addAttribute("clients", clients.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clients.getTotalPages());
        model.addAttribute("size", size);
        return "admin/clients";
    }

    /**
     * Страница управления записями.
     *
     * @param page номер страницы
     * @param size размер страницы
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/appointments")
    public String appointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<AppointmentDto> appointments = coreServiceClient.getAllAppointmentsForAdmin(page, size, "appointmentDate", "desc");
        model.addAttribute("appointments", appointments.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appointments.getTotalPages());
        model.addAttribute("size", size);
        return "admin/appointments";
    }

    /**
     * Страница управления мастерами.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/masters")
    public String masters(Model model) {
        java.util.List<MasterDto> masters = coreServiceClient.getAllMasters();
        model.addAttribute("masters", masters);
        return "admin/masters";
    }

    /**
     * Профиль администратора.
     *
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона или "error" при ошибке
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String email = authentication.getName();
        try {
            MasterDto admin = coreServiceClient.getMasterByEmail(email);
            model.addAttribute("admin", admin);
            return "admin/profile";
        } catch (Exception e) {
            log.error("Error loading admin profile: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Подтверждение записи.
     *
     * @param id идентификатор записи
     * @return редирект на страницу записей
     */
    @PostMapping("/appointments/{id}/confirm")
    public String confirmAppointment(@PathVariable Long id) {
        coreServiceClient.updateAppointmentStatus(id, "CONFIRMED");
        return "redirect:/admin/appointments";
    }

    /**
     * Завершение записи.
     *
     * @param id идентификатор записи
     * @return редирект на страницу записей
     */
    @PostMapping("/appointments/{id}/complete")
    public String completeAppointment(@PathVariable Long id) {
        coreServiceClient.updateAppointmentStatus(id, "COMPLETED");
        return "redirect:/admin/appointments";
    }

    /**
     * Отмена записи.
     *
     * @param id идентификатор записи
     * @return редирект на страницу записей
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id) {
        coreServiceClient.updateAppointmentStatus(id, "CANCELLED");
        return "redirect:/admin/appointments";
    }
}