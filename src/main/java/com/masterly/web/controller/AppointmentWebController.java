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

import java.time.LocalDate;
import java.util.List;

/**
 * Контроллер управления записями для мастера.
 */
@Slf4j
@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentWebController {

    private final CoreServiceClient coreServiceClient;

    private Long getMasterId(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication required");
        }

        String email = authentication.getName();
        log.debug("Getting master ID for email: {}", email);

        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);
            return master.getId();
        } catch (Exception e) {
            log.error("Error getting master ID: {}", e.getMessage());
            throw new RuntimeException("Master not found for email: " + email);
        }
    }

    /**
     * Список записей мастера.
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
    public String listAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            Model model) {

        log.debug("Listing appointments - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<AppointmentDto> appointmentPage;

        if (isAdmin) {
            log.info("Admin viewing all appointments");
            appointmentPage = coreServiceClient.getAllAppointmentsForAdmin(page, size, sortBy, sortDir);
        } else {
            Long masterId = getMasterId(authentication);
            log.debug("Master {} viewing appointments", masterId);
            appointmentPage = coreServiceClient.getAppointmentsPaginated(page, size, sortBy, sortDir, masterId);
        }

        log.debug("Found {} appointments total", appointmentPage.getTotalElements());

        model.addAttribute("appointments", appointmentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appointmentPage.getTotalPages());
        model.addAttribute("totalItems", appointmentPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "appointments/list";
    }

    /**
     * Сохранение записи.
     *
     * @param createDto данные для создания записи
     * @param id идентификатор записи (опционально, для обновления)
     * @return редирект на список записей
     */
    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute AppointmentCreateDto createDto,
                                  @RequestParam(required = false) Long id) {

        if (id == null) {
            log.info("Creating new appointment for client: {}, service: {}, date: {}, time: {}",
                    createDto.getClientId(), createDto.getServiceId(),
                    createDto.getAppointmentDate(), createDto.getStartTime());
        } else {
            log.info("Updating appointment: {}", id);
        }

        createDto.setMasterId(1L);

        if (id == null) {
            coreServiceClient.createAppointment(createDto);
            log.debug("Appointment created successfully");
        } else {
            coreServiceClient.updateAppointment(id, createDto);
            log.debug("Appointment {} updated successfully", id);
        }

        return "redirect:/appointments";
    }

    /**
     * Удаление записи.
     *
     * @param id идентификатор записи
     * @return редирект на список записей
     */
    @GetMapping("/delete/{id}")
    public String deleteAppointment(@PathVariable Long id) {
        log.info("Deleting appointment: {}", id);

        coreServiceClient.deleteAppointment(id);

        log.debug("Appointment {} deleted", id);
        return "redirect:/appointments";
    }

    /**
     * Обновление статуса записи.
     *
     * @param id идентификатор записи
     * @param status новый статус
     * @return редирект на список записей
     */
    @GetMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("Updating appointment {} status to: {}", id, status);

        coreServiceClient.updateAppointmentStatus(id, status);

        log.debug("Appointment {} status updated to: {}", id, status);
        return "redirect:/appointments";
    }

    /**
     * Форма редактирования записи.
     *
     * @param id идентификатор записи
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        log.debug("Showing edit form for appointment: {}", id);

        Long masterId = 1L;

        AppointmentDto appointment = coreServiceClient.getAppointment(id);
        log.debug("Loaded appointment: client={}, service={}, date={}, time={}",
                appointment.getClientId(), appointment.getServiceId(),
                appointment.getAppointmentDate(), appointment.getStartTime());

        List<ClientDto> clients = coreServiceClient.getAllClients(masterId);
        List<ServiceEntityDto> services = coreServiceClient.getAllServices(masterId);
        log.debug("Loaded {} clients and {} services for edit form", clients.size(), services.size());

        AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                .clientId(appointment.getClientId())
                .serviceId(appointment.getServiceId())
                .appointmentDate(appointment.getAppointmentDate())
                .startTime(appointment.getStartTime())
                .notes(appointment.getNotes())
                .build();

        model.addAttribute("appointment", createDto);
        model.addAttribute("appointmentId", id);
        model.addAttribute("clients", clients);
        model.addAttribute("services", services);
        model.addAttribute("today", LocalDate.now());

        return "appointments/form";
    }
}