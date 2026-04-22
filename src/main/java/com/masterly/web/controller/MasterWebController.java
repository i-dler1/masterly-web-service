package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер панели мастера.
 */
@Slf4j
@Controller
@RequestMapping("/master")
@RequiredArgsConstructor
public class MasterWebController {

    private final CoreServiceClient coreServiceClient;

    private Long getMasterId(Authentication authentication) {
        String email = authentication.getName();
        try {
            MasterDto master = coreServiceClient.getMasterByEmail(email);
            return master.getId();
        } catch (Exception e) {
            log.error("Error getting master ID: {}", e.getMessage());
            return 1L;
        }
    }

    /**
     * Дашборд мастера.
     *
     * @return название шаблона
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "masters/dashboard";
    }

    // ========== УСЛУГИ МАСТЕРА ==========

    /**
     * Список услуг мастера с пагинацией.
     *
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @param page номер страницы
     * @param size размер страницы
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки
     * @return название шаблона
     */
    @GetMapping("/services")
    public String services(Model model, Authentication auth,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "name") String sortBy,
                           @RequestParam(defaultValue = "asc") String sortDir) {
        Long masterId = getMasterId(auth);

        log.info("=== SERVICES PAGINATION ===");
        log.info("page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);

        Page<ServiceEntityDto> services = coreServiceClient.getServicesPaginated(page, size, sortBy, sortDir, masterId);

        log.info("services content size: {}", services.getContent().size());
        log.info("totalPages: {}", services.getTotalPages());
        log.info("totalElements: {}", services.getTotalElements());
        log.info("currentPage: {}", services.getNumber());

        model.addAttribute("services", services.getContent());
        model.addAttribute("currentPage", services.getNumber());
        model.addAttribute("totalPages", services.getTotalPages());
        model.addAttribute("totalItems", services.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "masters/services";
    }

    /**
     * Форма создания услуги.
     *
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @return название шаблона
     */
    @GetMapping("/services/new")
    public String newServiceForm(Model model, Authentication auth) {
        log.info("=== NEW SERVICE FORM ===");
        model.addAttribute("service", new ServiceEntityDto());

        Long masterId = getMasterId(auth);
        log.info("Master ID: {}", masterId);

        try {
            List<MaterialDto> allMaterials = coreServiceClient.getAllMaterials(masterId);
            log.info("Materials loaded: {}", allMaterials != null ? allMaterials.size() : 0);
            model.addAttribute("allMaterials", allMaterials);
        } catch (Exception e) {
            log.error("Error loading materials: {}", e.getMessage());
            model.addAttribute("allMaterials", List.of());
        }

        return "masters/service-form";
    }

    /**
     * Форма редактирования услуги.
     *
     * @param id идентификатор услуги
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/services/edit/{id}")
    public String editServiceForm(@PathVariable Long id, Authentication auth, Model model) {
        log.info("=== EDIT SERVICE FORM ===");
        Long masterId = getMasterId(auth);
        ServiceEntityDto service = coreServiceClient.getService(id, masterId);
        model.addAttribute("service", service);

        List<MaterialDto> allMaterials = coreServiceClient.getAllMaterials(masterId);
        model.addAttribute("allMaterials", allMaterials);

        List<ServiceMaterialDto> serviceMaterials = coreServiceClient.getServiceMaterials(id);
        model.addAttribute("serviceMaterials", serviceMaterials);

        return "masters/service-form";
    }

    /**
     * Сохранение услуги.
     *
     * @param serviceDto данные услуги
     * @param auth данные аутентификации
     * @return редирект на список услуг
     */
    @PostMapping("/services/save")
    public String saveService(@ModelAttribute ServiceEntityDto serviceDto, Authentication auth) {
        Long masterId = getMasterId(auth);
        if (serviceDto.getId() == null) {
            coreServiceClient.createService(masterId, serviceDto);
        } else {
            coreServiceClient.updateService(serviceDto.getId(), masterId, serviceDto);
        }
        return "redirect:/master/services";
    }

    /**
     * Удаление услуги.
     *
     * @param id идентификатор услуги
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список услуг
     */
    @GetMapping("/services/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("=== DELETE SERVICE MASTER CALLED FOR ID: {} ===", id);
        try {
            coreServiceClient.deleteService(id);
            redirectAttributes.addFlashAttribute("success", "Услуга успешно удалена");
            log.info("Service {} deleted successfully", id);
        } catch (feign.FeignException e) {
            log.error("Feign exception: status={}, message={}", e.status(), e.getMessage());

            if (e.status() == 409) {
                try {
                    coreServiceClient.deactivateService(id);
                    log.info("Service {} deactivated (moved to archive)", id);
                    redirectAttributes.addFlashAttribute("warning",
                            "Услуга перемещена в архив (на неё есть записи). При необходимости её можно восстановить.");
                } catch (Exception ex) {
                    log.error("Deactivation failed: {}", ex.getMessage());
                    redirectAttributes.addFlashAttribute("error", "Ошибка при архивации услуги");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении услуги: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении услуги");
        }
        return "redirect:/master/services";
    }

    /**
     * Активация услуги из архива.
     *
     * @param id идентификатор услуги
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список услуг
     */
    @GetMapping("/services/activate/{id}")
    public String activateService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            coreServiceClient.activateService(id);
            redirectAttributes.addFlashAttribute("success", "Услуга восстановлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при восстановлении");
        }
        return "redirect:/master/services";
    }

    // ========== МАТЕРИАЛЫ МАСТЕРА ==========

    /**
     * Список материалов мастера.
     *
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @param page номер страницы
     * @param size размер страницы
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки
     * @return название шаблона
     */
    @GetMapping("/materials")
    public String materials(Model model, Authentication auth,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(defaultValue = "name") String sortBy,
                            @RequestParam(defaultValue = "asc") String sortDir) {
        Long masterId = getMasterId(auth);

        Page<MaterialDto> materials = coreServiceClient.getMaterialsPaginated(page, size, sortBy, sortDir, masterId);

        model.addAttribute("materials", materials.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", materials.getTotalPages());
        model.addAttribute("totalItems", materials.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "masters/materials";
    }

    /**
     * Форма создания материала.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/materials/new")
    public String newMaterialForm(Model model) {
        model.addAttribute("material", new MaterialDto());
        return "masters/material-form";
    }

    /**
     * Форма редактирования материала.
     *
     * @param id идентификатор материала
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/materials/edit/{id}")
    public String editMaterialForm(@PathVariable Long id, Authentication auth, Model model) {
        Long masterId = getMasterId(auth);
        MaterialDto material = coreServiceClient.getMaterial(id, masterId);
        model.addAttribute("material", material);
        return "masters/material-form";
    }

    /**
     * Сохранение материала.
     *
     * @param materialDto данные материала
     * @param auth данные аутентификации
     * @return редирект на список материалов
     */
    @PostMapping("/materials/save")
    public String saveMaterial(@ModelAttribute MaterialDto materialDto, Authentication auth) {
        Long masterId = getMasterId(auth);
        if (materialDto.getId() == null) {
            coreServiceClient.createMaterial(masterId, materialDto);
        } else {
            coreServiceClient.updateMaterial(materialDto.getId(), masterId, materialDto);
        }
        return "redirect:/master/materials";
    }

    /**
     * Удаление материала.
     *
     * @param id идентификатор материала
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список материалов
     */
    @GetMapping("/materials/delete/{id}")
    public String deleteMaterial(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        Long masterId = getMasterId(auth);
        try {
            coreServiceClient.deleteMaterial(id);
            redirectAttributes.addFlashAttribute("success", "Материал удален");
        } catch (feign.FeignException e) {
            if (e.status() == 409) {
                redirectAttributes.addFlashAttribute("error", "Нельзя удалить материал, он используется в услугах");
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении");
            }
        }
        return "redirect:/master/materials";
    }

    // ========== КЛИЕНТЫ МАСТЕРА ==========

    /**
     * Список клиентов мастера.
     *
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @param page номер страницы
     * @param size размер страницы
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки
     * @return название шаблона
     */
    @GetMapping("/clients")
    public String clients(Model model, Authentication auth,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(defaultValue = "fullName") String sortBy,
                          @RequestParam(defaultValue = "asc") String sortDir) {
        Long masterId = getMasterId(auth);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("fullName").ascending());
        Page<ClientDto> clients = coreServiceClient.getClientsPaginated(page, size, sortBy, sortDir, masterId);

        model.addAttribute("clients", clients.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", clients.getTotalPages());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "masters/clients";
    }

    /**
     * Форма создания клиента.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/clients/new")
    public String newClientForm(Model model) {
        model.addAttribute("client", new ClientDto());
        return "masters/client-form";
    }

    /**
     * Форма редактирования клиента.
     *
     * @param id идентификатор клиента
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/clients/edit/{id}")
    public String editClientForm(@PathVariable Long id, Authentication auth, Model model) {
        Long masterId = getMasterId(auth);
        ClientDto client = coreServiceClient.getClient(id, masterId);
        model.addAttribute("client", client);
        return "masters/client-form";
    }

    /**
     * Сохранение клиента.
     *
     * @param clientDto данные клиента
     * @param auth данные аутентификации
     * @return редирект на список клиентов
     */
    @PostMapping("/clients/save")
    public String saveClient(@ModelAttribute ClientDto clientDto, Authentication auth) {
        Long masterId = getMasterId(auth);
        if (clientDto.getId() == null) {
            coreServiceClient.createClient(masterId, clientDto);
        } else {
            coreServiceClient.updateClient(clientDto.getId(), masterId, clientDto);
        }
        return "redirect:/master/clients";
    }

    /**
     * Удаление клиента.
     *
     * @param id идентификатор клиента
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список клиентов
     */
    @GetMapping("/clients/delete/{id}")
    public String deleteClient(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        Long masterId = getMasterId(auth);
        try {
            coreServiceClient.deleteClient(id, masterId);
            redirectAttributes.addFlashAttribute("success", "Клиент успешно удален");
        } catch (feign.FeignException e) {
            log.error("Delete client error: status={}, message={}", e.status(), e.getMessage());
            if (e.status() == 409) {
                redirectAttributes.addFlashAttribute("error", "Нельзя удалить клиента, так как у него есть записи");
            } else {
                redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
            }
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении клиента");
        }
        return "redirect:/master/clients";
    }

    // ========== ЗАПИСИ ==========

    /**
     * Список записей мастера.
     *
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @param page номер страницы
     * @param size размер страницы
     * @param sortBy поле для сортировки
     * @param sortDir направление сортировки
     * @return название шаблона
     */
    @GetMapping("/appointments")
    public String appointments(Model model, Authentication auth,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "appointmentDate") String sortBy,
                               @RequestParam(defaultValue = "desc") String sortDir) {
        Long masterId = getMasterId(auth);

        Page<AppointmentDto> appointmentsPage = coreServiceClient.getAppointmentsPaginated(page, size, sortBy, sortDir, masterId);

        model.addAttribute("appointments", appointmentsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appointmentsPage.getTotalPages());
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("today", LocalDate.now());

        return "masters/appointments";
    }

    /**
     * Подтверждение записи.
     *
     * @param id идентификатор записи
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список записей
     */
    @PostMapping("/appointments/{id}/confirm")
    public String confirmAppointment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Confirming appointment: {}", id);
        try {
            coreServiceClient.updateAppointmentStatus(id, "CONFIRMED");
            redirectAttributes.addFlashAttribute("success", "Запись подтверждена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при подтверждении");
        }
        return "redirect:/master/appointments";
    }

    /**
     * Отмена записи.
     *
     * @param id идентификатор записи
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список записей
     */
    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        log.info("Cancelling appointment: {}", id);
        try {
            coreServiceClient.updateAppointmentStatus(id, "CANCELLED");
            redirectAttributes.addFlashAttribute("success", "Запись отменена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отмене");
        }
        return "redirect:/master/appointments";
    }

    /**
     * Календарь записей.
     *
     * @param view вид отображения (week/month)
     * @param date текущая дата
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/appointments/calendar")
    public String appointmentsCalendar(@RequestParam(required = false) String view,
                                       @RequestParam(required = false) String date,
                                       Authentication auth,
                                       Model model) {
        Long masterId = getMasterId(auth);
        log.info("=== CALENDAR PAGE ===");
        log.info("Master ID: {}", masterId);

        LocalDate currentDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        String currentView = view != null ? view : "week";

        List<AppointmentDto> allAppointments = coreServiceClient.getAppointmentsByMasterId(masterId);
        log.info("Appointments count: {}", allAppointments.size());

        List<AvailabilitySlotDto> allSlots = coreServiceClient.getAllSlots(masterId);
        log.info("Slots count: {}", allSlots.size());

        List<ServiceEntityDto> allServices = null;
        try {
            allServices = coreServiceClient.getAllServices(masterId);
            log.info("Services count: {}", allServices.size());
            log.info("Services: {}", allServices);
        } catch (Exception e) {
            log.error("Error getting services: {}", e.getMessage());
            e.printStackTrace();
            allServices = List.of();
        }

        LocalDate startDate = currentView.equals("week") ?
                currentDate.with(DayOfWeek.MONDAY) : currentDate.withDayOfMonth(1);

        List<LocalDate> daysInRange = new ArrayList<>();
        if (currentView.equals("month")) {
            LocalDate firstDay = currentDate.withDayOfMonth(1);
            LocalDate lastDay = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            LocalDate start = firstDay.with(DayOfWeek.MONDAY);
            LocalDate end = lastDay.with(DayOfWeek.SUNDAY);

            LocalDate day = start;
            while (!day.isAfter(end)) {
                daysInRange.add(day);
                day = day.plusDays(1);
            }
        }

        model.addAttribute("appointments", allAppointments);
        model.addAttribute("slots", allSlots);
        model.addAttribute("services", allServices);
        model.addAttribute("currentDate", currentDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("daysInRange", daysInRange);
        model.addAttribute("view", currentView);
        model.addAttribute("monthName", getMonthName(currentDate.getMonthValue()));
        model.addAttribute("year", currentDate.getYear());
        model.addAttribute("today", LocalDate.now());

        return "masters/appointments-calendar";
    }

    /**
     * Страница управления слотами.
     *
     * @return название шаблона
     */
    @GetMapping("/slots")
    public String slots() {
        return "masters/slots";
    }

    /**
     * Профиль мастера.
     *
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        Long masterId = getMasterId(auth);
        MasterDto master = coreServiceClient.getMasterById(masterId);
        model.addAttribute("master", master);
        return "masters/profile";
    }

    /**
     * Форма редактирования профиля.
     *
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/profile/edit")
    public String showEditProfileForm(Authentication auth, Model model) {
        Long masterId = getMasterId(auth);
        MasterDto master = coreServiceClient.getMasterById(masterId);
        model.addAttribute("master", master);
        return "masters/profile-edit";
    }

    /**
     * Обновление профиля.
     *
     * @param masterDto данные мастера
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на профиль
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute MasterDto masterDto, Authentication auth, RedirectAttributes redirectAttributes) {
        Long masterId = getMasterId(auth);
        try {
            MasterUpdateDto updateDto = MasterUpdateDto.builder()
                    .fullName(masterDto.getFullName())
                    .phone(masterDto.getPhone())
                    .businessName(masterDto.getBusinessName())
                    .specialization(masterDto.getSpecialization())
                    .build();
            coreServiceClient.updateMasterProfile(masterId, updateDto);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении профиля");
        }
        return "redirect:/master/profile";
    }

    // ========== API ДЛЯ AJAX ==========

    /**
     * Получение данных об услугах (API).
     *
     * @param auth данные аутентификации
     * @return список услуг
     */
    @GetMapping("/services-data")
    @ResponseBody
    public List<ServiceEntityDto> getServicesData(Authentication auth) {
        Long masterId = getMasterId(auth);
        return coreServiceClient.getAllServices(masterId);
    }

    /**
     * Получение слотов по дате (API).
     *
     * @param masterId идентификатор мастера
     * @param date дата
     * @param serviceId идентификатор услуги (опционально)
     * @param auth данные аутентификации
     * @return список слотов
     */
    @GetMapping("/api/slots")
    @ResponseBody
    public List<AvailabilitySlotDto> getSlots(@RequestParam Long masterId,
                                              @RequestParam String date,
                                              @RequestParam(required = false) Long serviceId,
                                              Authentication auth) {
        log.info("Getting slots for masterId={}, date={}, serviceId={}", masterId, date, serviceId);

        if (serviceId == null) {
            return coreServiceClient.getFreeSlots(masterId, null, date);
        }

        return coreServiceClient.getFreeSlots(masterId, serviceId, date);
    }

    /**
     * Создание слота (API).
     *
     * @param slotDto данные слота
     * @param auth данные аутентификации
     * @return созданный слот
     */
    @PostMapping("/api/slots")
    @ResponseBody
    public AvailabilitySlotDto createSlot(@RequestBody AvailabilitySlotDto slotDto, Authentication auth) {
        Long masterId = getMasterId(auth);

        LocalDate slotDate = slotDto.getSlotDate();
        if (slotDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Нельзя создать слот на прошедшую дату");
        }

        LocalTime startTime = slotDto.getStartTime();
        LocalTime endTime = slotDto.getEndTime();
        if (!startTime.isBefore(endTime)) {
            throw new RuntimeException("Время начала должно быть раньше времени окончания");
        }

        ServiceEntityDto service = coreServiceClient.getService(slotDto.getServiceId(), masterId);
        if (service == null || !service.getIsActive()) {
            throw new RuntimeException("Услуга неактивна или не найдена. Нельзя создать слот для архивной услуги.");
        }

        slotDto.setMasterId(masterId);
        return coreServiceClient.createSlot(slotDto);
    }

    /**
     * Удаление слота (API).
     *
     * @param slotId идентификатор слота
     * @param auth данные аутентификации
     */
    @DeleteMapping("/api/slots/{slotId}")
    @ResponseBody
    public void deleteSlot(@PathVariable Long slotId, Authentication auth) {
        log.info("Deleting slot: {}", slotId);
        Long masterId = getMasterId(auth);
        coreServiceClient.deleteSlot(slotId);
    }

    /**
     * Получение всех слотов (API).
     *
     * @param auth данные аутентификации
     * @return список слотов
     */
    @GetMapping("/api/slots/all")
    @ResponseBody
    public List<AvailabilitySlotDto> getAllSlots(Authentication auth) {
        Long masterId = getMasterId(auth);
        return coreServiceClient.getAllSlots(masterId);
    }

    /**
     * Получение слотов по дате (API).
     *
     * @param date дата
     * @param auth данные аутентификации
     * @return список слотов
     */
    @GetMapping("/api/slots/by-date")
    @ResponseBody
    public List<AvailabilitySlotDto> getSlotsByDate(@RequestParam String date, Authentication auth) {
        Long masterId = getMasterId(auth);
        log.info("Getting slots for masterId={}, date={}", masterId, date);

        return coreServiceClient.getFreeSlots(masterId, null, date);
    }

    /**
     * Получение активных услуг (API).
     *
     * @param auth данные аутентификации
     * @return список активных услуг
     */
    @GetMapping("/api/services/active")
    @ResponseBody
    public List<ServiceEntityDto> getActiveServices(Authentication auth) {
        Long masterId = getMasterId(auth);
        List<ServiceEntityDto> allServices = coreServiceClient.getAllServices(masterId);
        return allServices.stream()
                .filter(ServiceEntityDto::getIsActive)
                .collect(Collectors.toList());
    }

    /**
     * Получение всех материалов (API).
     *
     * @param auth данные аутентификации
     * @return список материалов
     */
    @GetMapping("/api/materials/all")
    @ResponseBody
    public List<MaterialDto> getAllMaterials(Authentication auth) {
        Long masterId = getMasterId(auth);
        return coreServiceClient.getAllMaterials(masterId);
    }

    // ========== УПРАВЛЕНИЕ МАТЕРИАЛАМИ УСЛУГИ ==========

    /**
     * Страница управления материалами услуги.
     *
     * @param id идентификатор услуги
     * @param auth данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/services/{id}/materials")
    public String serviceMaterials(@PathVariable Long id, Authentication auth, Model model) {
        Long masterId = getMasterId(auth);

        ServiceEntityDto service = coreServiceClient.getService(id, masterId);
        model.addAttribute("serviceId", id);
        model.addAttribute("serviceName", service.getName());

        List<MaterialDto> allMaterials = coreServiceClient.getAllMaterials(masterId);
        model.addAttribute("allMaterials", allMaterials);

        List<ServiceMaterialDto> serviceMaterials = coreServiceClient.getServiceMaterials(id);
        model.addAttribute("serviceMaterials", serviceMaterials);

        return "masters/service-materials";
    }

    /**
     * Добавление материала к услуге.
     *
     * @param serviceId идентификатор услуги
     * @param materialId идентификатор материала
     * @param quantityUsed количество
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу материалов услуги
     */
    @PostMapping("/services/{serviceId}/materials/add")
    public String addMaterialToService(@PathVariable Long serviceId,
                                       @RequestParam Long materialId,
                                       @RequestParam BigDecimal quantityUsed,
                                       RedirectAttributes redirectAttributes) {
        try {
            coreServiceClient.addMaterialToService(serviceId, materialId, quantityUsed, null);
            redirectAttributes.addFlashAttribute("success", "Материал добавлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при добавлении материала");
        }
        return "redirect:/master/services/" + serviceId + "/materials";
    }

    /**
     * Удаление материала из услуги.
     *
     * @param serviceId идентификатор услуги
     * @param id идентификатор связи
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на страницу материалов услуги
     */
    @GetMapping("/services/{serviceId}/materials/{id}/remove")
    public String removeMaterialFromService(@PathVariable Long serviceId,
                                            @PathVariable Long id,
                                            RedirectAttributes redirectAttributes) {
        try {
            coreServiceClient.removeMaterialFromService(id);
            redirectAttributes.addFlashAttribute("success", "Материал удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при удалении материала");
        }
        return "redirect:/master/services/" + serviceId + "/materials";
    }

    // ========== ЗАПИСЬ ДЛЯ КЛИЕНТА (МАСТЕР СОЗДАЕТ) ==========

    /**
     * Форма создания записи для клиента.
     *
     * @param id идентификатор клиента
     * @param model модель для передачи данных в шаблон
     * @param auth данные аутентификации
     * @return название шаблона
     */
    @GetMapping("/clients/{id}/book")
    public String bookForClient(@PathVariable Long id, Model model, Authentication auth) {
        Long masterId = getMasterId(auth);

        ClientDto client = coreServiceClient.getClient(id, masterId);
        model.addAttribute("client", client);

        List<ServiceEntityDto> services = coreServiceClient.getAllServices(masterId);
        model.addAttribute("services", services);

        return "masters/book-for-client";
    }

    /**
     * Сохранение записи для клиента.
     *
     * @param id идентификатор клиента
     * @param serviceId идентификатор услуги
     * @param appointmentDate дата записи
     * @param startTime время начала
     * @param notes примечание
     * @param auth данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return редирект на список клиентов
     */
    @PostMapping("/clients/{id}/book/save")
    public String saveBookingForClient(@PathVariable Long id,
                                       @RequestParam Long serviceId,
                                       @RequestParam String appointmentDate,
                                       @RequestParam String startTime,
                                       @RequestParam(required = false) String notes,
                                       Authentication auth,
                                       RedirectAttributes redirectAttributes) {
        Long masterId = getMasterId(auth);

        try {
            AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                    .masterId(masterId)
                    .clientId(id)
                    .serviceId(serviceId)
                    .appointmentDate(LocalDate.parse(appointmentDate))
                    .startTime(LocalTime.parse(startTime))
                    .notes(notes)
                    .build();

            coreServiceClient.createAppointment(createDto);
            redirectAttributes.addFlashAttribute("success", "Запись успешно создана!");

        } catch (Exception e) {
            log.error("Error creating appointment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании записи: " + e.getMessage());
        }

        return "redirect:/master/clients";
    }

    private String getMonthName(int month) {
        String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
        return months[month - 1];
    }
}