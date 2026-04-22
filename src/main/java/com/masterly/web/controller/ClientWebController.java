package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Контроллер клиентской панели.
 */
@Slf4j
@Controller
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientWebController {

    private final CoreServiceClient coreServiceClient;

    private Long getMasterId(Authentication authentication) {
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
     * Список клиентов мастера.
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
    public String listClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication,
            Model model) {

        log.debug("Listing clients - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<ClientDto> clientPage;

        if (isAdmin) {
            log.info("Admin viewing all clients");
            clientPage = coreServiceClient.getAllClientsForAdmin(page, size, sortBy, sortDir);
        } else {
            Long masterId = getMasterId(authentication);
            log.debug("Master {} viewing clients", masterId);
            clientPage = coreServiceClient.getClientsPaginated(page, size, sortBy, sortDir, masterId);
        }

        log.debug("Found {} clients total", clientPage.getTotalElements());

        model.addAttribute("clients", clientPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clientPage.getTotalPages());
        model.addAttribute("totalItems", clientPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "clients/list";
    }

    /**
     * Форма создания клиента.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        log.debug("Showing create client form");
        model.addAttribute("client", new ClientDto());
        return "clients/form";
    }

    /**
     * Форма редактирования клиента.
     *
     * @param id идентификатор клиента
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Authentication authentication,
                               Model model) {
        log.debug("Showing edit form for client: {}", id);
        Long masterId = getMasterId(authentication);
        ClientDto client = coreServiceClient.getClient(id, masterId);
        log.debug("Loaded client: {}", client.getFullName());
        model.addAttribute("client", client);
        return "clients/form";
    }

    /**
     * Сохранение клиента.
     *
     * @param clientDto данные клиента
     * @param result результат валидации
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return редирект на список клиентов или возврат к форме при ошибке
     */
    @PostMapping("/save")
    public String saveClient(@Valid @ModelAttribute("client") ClientDto clientDto,
                             BindingResult result,
                             Authentication authentication,
                             Model model) {

        if (result.hasErrors()) {
            log.warn("Validation errors while saving client: {}", result.getAllErrors());
            return "clients/form";
        }

        Long masterId = getMasterId(authentication);

        if (clientDto.getId() == null) {
            log.info("Creating new client: {}", clientDto.getFullName());
            coreServiceClient.createClient(masterId, clientDto);
            log.debug("Client created successfully");
        } else {
            log.info("Updating client: {}", clientDto.getId());
            coreServiceClient.updateClient(clientDto.getId(), masterId, clientDto);
            log.debug("Client {} updated successfully", clientDto.getId());
        }

        return "redirect:/clients";
    }

    /**
     * Удаление клиента.
     *
     * @param id идентификатор клиента
     * @param masterId идентификатор мастера
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список клиентов
     */
    @GetMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id,
                               @RequestParam Long masterId,
                               RedirectAttributes redirectAttributes) {
        log.info("Deleting client: {}", id);

        try {
            coreServiceClient.deleteClient(id, masterId);
            log.debug("Client {} deleted successfully", id);
            redirectAttributes.addFlashAttribute("success", "Клиент успешно удален");
            return "redirect:/clients?masterId=" + masterId;

        } catch (feign.FeignException e) {
            if (e.status() == 403 || e.status() == 400) {
                log.warn("Cannot delete client: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("error", "Нельзя удалить клиента, так как у него есть записи");
            } else {
                log.error("Error deleting client: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
            }
            return "redirect:/clients?masterId=" + masterId;

        } catch (Exception e) {
            log.error("Error deleting client: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
            return "redirect:/clients?masterId=" + masterId;
        }
    }

    /**
     * Профиль клиента.
     *
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона или "error" при ошибке
     */
    @GetMapping("/profile")
    public String clientProfile(Authentication authentication, Model model) {
        String email = authentication.getName();
        log.info("Client profile access for: {}", email);

        try {
            ClientDto client = coreServiceClient.getClientByEmail(email);
            model.addAttribute("client", client);
            return "clients/profile";
        } catch (Exception e) {
            log.error("Error loading client profile: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Записи клиента.
     *
     * @param authentication данные аутентификации
     * @param newAppointmentId идентификатор новой записи (опционально)
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/appointments")
    public String myAppointments(Authentication authentication,
                                 @ModelAttribute(value = "newAppointmentId") String newAppointmentId,
                                 Model model) {
        String email = authentication.getName();
        ClientDto client = coreServiceClient.getClientByEmail(email);
        List<AppointmentDto> appointments = coreServiceClient.getAppointmentsByClientId(client.getId());

        appointments.sort((a, b) -> {
            LocalDate today = LocalDate.now();
            boolean aPast = a.getAppointmentDate().isBefore(today);
            boolean bPast = b.getAppointmentDate().isBefore(today);

            if (aPast && !bPast) return 1;
            if (!aPast && bPast) return -1;
            return b.getAppointmentDate().compareTo(a.getAppointmentDate());
        });

        Long newId = null;
        if (newAppointmentId != null && !newAppointmentId.isEmpty()) {
            try {
                newId = Long.parseLong(newAppointmentId);
            } catch (NumberFormatException e) {
                log.warn("Invalid newAppointmentId: {}", newAppointmentId);
            }
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("newAppointmentId", newId);
        model.addAttribute("today", LocalDate.now());

        return "clients/appointments";
    }

    /**
     * Дашборд клиента.
     *
     * @return название шаблона
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "clients/dashboard";
    }

    /**
     * Список мастеров для клиента.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/masters")
    public String masters(Model model) {
        List<MasterDto> masters = coreServiceClient.getAllMasters();
        model.addAttribute("masters", masters);
        return "clients/masters";
    }

    /**
     * Просмотр профиля мастера клиентом.
     *
     * @param id идентификатор мастера
     * @param model модель для передачи данных в шаблон
     * @return название шаблона или "error" при ошибке
     */
    @GetMapping("/masters/{id}")
    public String viewMaster(@PathVariable Long id, Model model) {
        log.info("Client viewing master profile: {}", id);

        try {
            MasterDto master = coreServiceClient.getMasterById(id);
            model.addAttribute("master", master);

            List<ServiceEntityDto> allServices = coreServiceClient.getServicesByMasterId(id);
            List<ServiceEntityDto> activeServices = allServices.stream()
                    .filter(ServiceEntityDto::getIsActive)
                    .collect(Collectors.toList());
            model.addAttribute("services", activeServices);

            return "clients/master-details";
        } catch (Exception e) {
            log.error("Error loading master details: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Форма бронирования услуги.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @param redirectAttributes атрибуты для редиректа
     * @return название шаблона или редирект при ошибке
     */
    @GetMapping("/masters/{masterId}/book/{serviceId}")
    public String showBookingForm(@PathVariable Long masterId,
                                  @PathVariable Long serviceId,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        log.info("Client booking service: masterId={}, serviceId={}", masterId, serviceId);

        try {
            ServiceEntityDto serviceCheck = coreServiceClient.getServiceById(serviceId);
            if (serviceCheck == null || !serviceCheck.getIsActive()) {
                log.warn("Service {} is not active or not found", serviceId);
                redirectAttributes.addFlashAttribute("error", "Данная услуга недоступна для записи");
                return "redirect:/client/masters/" + masterId;
            }

            String email = authentication.getName();
            ClientDto client = coreServiceClient.getClientByEmail(email);
            MasterDto master = coreServiceClient.getMasterById(masterId);
            ServiceEntityDto service = coreServiceClient.getServiceById(serviceId);

            model.addAttribute("client", client);
            model.addAttribute("master", master);
            model.addAttribute("service", service);

            return "clients/booking-form";
        } catch (Exception e) {
            log.error("Error loading booking form: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Получение доступных слотов (API).
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param date дата
     * @return список слотов
     */
    @GetMapping("/masters/{masterId}/slots")
    @ResponseBody
    public List<AvailabilitySlotDto> getAvailableSlots(@PathVariable Long masterId,
                                                       @RequestParam Long serviceId,
                                                       @RequestParam String date) {
        log.info("Getting available slots for masterId={}, serviceId={}, date={}", masterId, serviceId, date);

        try {
            ServiceEntityDto service = coreServiceClient.getService(serviceId, masterId);
            if (service == null || !service.getIsActive()) {
                log.warn("Service {} is not active, returning empty slots", serviceId);
                return List.of();
            }

            List<AvailabilitySlotDto> slots = coreServiceClient.getFreeSlots(masterId, serviceId, date);
            log.info("Found {} slots", slots != null ? slots.size() : 0);

            if (slots == null) {
                return List.of();
            }

            List<AvailabilitySlotDto> freeSlots = slots.stream()
                    .filter(slot -> slot.getIsBooked() == null || !slot.getIsBooked())
                    .collect(Collectors.toList());

            log.info("Returning {} free slots", freeSlots.size());
            return freeSlots;
        } catch (Exception e) {
            log.error("Error loading slots: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Создание записи через API.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param date дата
     * @return список слотов
     */
    @GetMapping("/api/slots")
    @ResponseBody
    public List<AvailabilitySlotDto> getFreeSlotsForClient(@RequestParam Long masterId,
                                                           @RequestParam Long serviceId,
                                                           @RequestParam String date) {
        return coreServiceClient.getFreeSlots(masterId, serviceId, date);
    }

    /**
     * Создание записи.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param slotId идентификатор слота
     * @param date дата
     * @param startTime время начала
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу записей
     */
    @GetMapping("/appointments/create")
    public String createAppointment(@RequestParam Long masterId,
                                    @RequestParam Long serviceId,
                                    @RequestParam Long slotId,
                                    @RequestParam String date,
                                    @RequestParam String startTime,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        log.info("Creating appointment - masterId: {}, serviceId: {}, slotId: {}, date: {}, startTime: {}",
                masterId, serviceId, slotId, date, startTime);

        try {
            String email = auth.getName();
            ClientDto client = coreServiceClient.getClientByEmail(email);

            AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                    .masterId(masterId)
                    .clientId(client.getId())
                    .serviceId(serviceId)
                    .appointmentDate(LocalDate.parse(date))
                    .startTime(LocalTime.parse(startTime))
                    .build();

            AppointmentDto newAppointment = coreServiceClient.createAppointment(createDto);

            redirectAttributes.addFlashAttribute("newAppointmentId", newAppointment.getId());
            redirectAttributes.addFlashAttribute("success", "✅ Запись успешно создана!");

            return "redirect:/client/appointments";

        } catch (Exception e) {
            log.error("Error creating appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "❌ Ошибка при создании записи: " + e.getMessage());
            return "redirect:/client/masters/" + masterId + "/book/" + serviceId + "/calendar?date=" + date + "&view=month";
        }
    }

    /**
     * Отмена записи клиентом.
     *
     * @param id идентификатор записи
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу записей
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        String email = auth.getName();
        ClientDto client = coreServiceClient.getClientByEmail(email);

        try {
            List<AppointmentDto> appointments = coreServiceClient.getAppointmentsByClientId(client.getId());
            AppointmentDto appointment = appointments.stream()
                    .filter(a -> a.getId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (appointment == null) {
                redirectAttributes.addFlashAttribute("error", "Запись не найдена");
                return "redirect:/client/appointments";
            }

            coreServiceClient.updateAppointmentStatus(id, "CANCELLED");
            coreServiceClient.releaseSlot(appointment.getMasterId(),
                    appointment.getAppointmentDate().toString(),
                    appointment.getStartTime().toString());

            redirectAttributes.addFlashAttribute("success", "Запись отменена, слот снова доступен");
        } catch (Exception e) {
            log.error("Error cancelling appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при отмене записи");
        }
        return "redirect:/client/appointments";
    }

    /**
     * Календарь бронирования.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param date текущая дата (опционально)
     * @param view вид отображения (week/month)
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @param redirectAttributes атрибуты для редиректа
     * @return название шаблона
     */
    @GetMapping("/masters/{masterId}/book/{serviceId}/calendar")
    public String showBookingCalendar(@PathVariable Long masterId,
                                      @PathVariable Long serviceId,
                                      @RequestParam(required = false) String date,
                                      @RequestParam(required = false) String view,
                                      Authentication auth,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {

        ServiceEntityDto serviceCheck = coreServiceClient.getServiceById(serviceId);
        if (serviceCheck == null || !serviceCheck.getIsActive()) {
            log.warn("Service {} is not active or not found", serviceId);
            redirectAttributes.addFlashAttribute("error", "Данная услуга недоступна для записи");
            return "redirect:/client/masters/" + masterId;
        }

        LocalDate currentDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        String currentView = view != null ? view : "month";

        log.info("=== BOOKING CALENDAR ===");
        log.info("MasterId: {}, ServiceId: {}, currentDate: {}, view: {}", masterId, serviceId, currentDate, currentView);

        MasterDto master = coreServiceClient.getMasterById(masterId);
        ServiceEntityDto service = coreServiceClient.getServiceById(serviceId);

        log.info("Master: {}, Service: {}", master.getFullName(), service.getName());

        Map<String, List<AvailabilitySlotDto>> slotsByDate = new HashMap<>();

        LocalDate startDate = currentView.equals("week") ?
                currentDate.with(DayOfWeek.MONDAY) : currentDate.withDayOfMonth(1);

        LocalDate endDate = currentView.equals("week") ?
                startDate.plusDays(6) : currentDate.withDayOfMonth(currentDate.lengthOfMonth());

        log.info("Date range: {} to {}", startDate, endDate);

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<AvailabilitySlotDto> slots = coreServiceClient.getFreeSlots(masterId, serviceId, current.toString());
            log.info("Date: {}, slots found: {}", current, slots.size());
            slotsByDate.put(current.toString(), slots);
            current = current.plusDays(1);
        }

        List<String> daysInRange = new ArrayList<>();
        if (currentView.equals("month")) {
            LocalDate firstDay = currentDate.withDayOfMonth(1);
            LocalDate lastDay = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            LocalDate start = firstDay.with(DayOfWeek.MONDAY);
            LocalDate end = lastDay.with(DayOfWeek.SUNDAY);

            LocalDate day = start;
            while (!day.isAfter(end)) {
                daysInRange.add(day.toString());
                day = day.plusDays(1);
            }
            log.info("Days in range for month view: {}", daysInRange.size());
        }

        model.addAttribute("masterId", masterId);
        model.addAttribute("masterName", master.getFullName());
        model.addAttribute("serviceId", serviceId);
        model.addAttribute("serviceName", service.getName());
        model.addAttribute("slotsByDate", slotsByDate);
        model.addAttribute("currentDate", currentDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("daysInRange", daysInRange);
        model.addAttribute("view", currentView);
        model.addAttribute("monthName", getMonthName(currentDate.getMonthValue()));
        model.addAttribute("year", currentDate.getYear());
        model.addAttribute("today", LocalDate.now());

        return "clients/booking-calendar";
    }

    private String getMonthName(int month) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month - 1];
    }

    /**
     * Редактирование профиля клиента.
     *
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/profile/edit")
    public String showEditProfileForm(Authentication auth, Model model) {
        String email = auth.getName();
        ClientDto client = coreServiceClient.getClientByEmail(email);
        model.addAttribute("client", client);
        return "clients/profile-edit";
    }

    /**
     * Обновление профиля клиента.
     *
     * @param clientDto данные клиента
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на профиль
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@Valid @ModelAttribute ClientDto clientDto,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        String email = auth.getName();
        ClientDto existingClient = coreServiceClient.getClientByEmail(email);

        clientDto.setId(existingClient.getId());
        coreServiceClient.updateClientProfile(clientDto.getId(), clientDto);

        redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        return "redirect:/client/profile";
    }

    /**
     * Доступные даты для записи.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @return список дат
     */
    @GetMapping("/masters/{masterId}/available-dates")
    @ResponseBody
    public List<String> getAvailableDates(@PathVariable Long masterId,
                                          @RequestParam Long serviceId) {
        log.info("Getting available dates for masterId={}, serviceId={}", masterId, serviceId);

        List<String> testDates = new ArrayList<>();
        testDates.add(LocalDate.now().plusDays(1).toString());
        testDates.add(LocalDate.now().plusDays(2).toString());

        return testDates;
    }

    /**
     * Бронирование услуги.
     *
     * @param masterId идентификатор мастера
     * @param serviceId идентификатор услуги
     * @param appointmentDate дата записи
     * @param startTime время начала
     * @param authentication данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу записей
     */
    @PostMapping("/masters/{masterId}/book/{serviceId}")
    public String bookService(@PathVariable Long masterId,
                              @PathVariable Long serviceId,
                              @RequestParam String appointmentDate,
                              @RequestParam String startTime,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        log.info("Booking service: masterId={}, serviceId={}, date={}, time={}",
                masterId, serviceId, appointmentDate, startTime);

        try {
            ServiceEntityDto service = coreServiceClient.getService(serviceId, masterId);
            if (service == null || !service.getIsActive()) {
                log.warn("Service {} is not active or not found", serviceId);
                redirectAttributes.addFlashAttribute("error", "Данная услуга недоступна для записи");
                return "redirect:/client/masters/" + masterId;
            }

            String email = authentication.getName();
            ClientDto client = coreServiceClient.getClientByEmail(email);

            AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                    .masterId(masterId)
                    .clientId(client.getId())
                    .serviceId(serviceId)
                    .appointmentDate(LocalDate.parse(appointmentDate))
                    .startTime(LocalTime.parse(startTime))
                    .build();

            coreServiceClient.createAppointment(createDto);

            redirectAttributes.addFlashAttribute("success", "Запись успешно создана!");
            return "redirect:/client/appointments";
        } catch (Exception e) {
            log.error("Error booking service: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании записи: " + e.getMessage());
            return "redirect:/client/masters/" + masterId;
        }
    }
}