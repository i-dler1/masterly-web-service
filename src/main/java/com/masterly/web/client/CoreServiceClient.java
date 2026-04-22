package com.masterly.web.client;

import com.masterly.web.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feign клиент для взаимодействия с core-service.
 */
@FeignClient(name = "core-service", url = "${core.service.url}")
public interface CoreServiceClient {

    // ==================== КЛИЕНТЫ ====================

    /**
     * Получить всех клиентов мастера.
     *
     * @param masterId ID мастера
     * @return страница с клиентами
     */
    @GetMapping("/api/clients")
    Page<ClientDto> getClients(@RequestParam("masterId") Long masterId);

    /**
     * Получить клиентов с пагинацией и сортировкой.
     *
     * @param page     номер страницы
     * @param size     размер страницы
     * @param sortBy   поле для сортировки
     * @param sortDir  направление сортировки
     * @param masterId ID мастера
     * @return страница с клиентами
     */
    @GetMapping("/api/clients/paginated")
    Page<ClientDto> getClientsPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("masterId") Long masterId
    );

    /**
     * Создать нового клиента.
     *
     * @param masterId  ID мастера
     * @param clientDto данные клиента
     * @return созданный клиент
     */
    @PostMapping("/api/clients")
    ClientDto createClient(@RequestParam("masterId") Long masterId, @RequestBody ClientDto clientDto);

    /**
     * Получить клиента по ID.
     *
     * @param id       ID клиента
     * @param masterId ID мастера
     * @return клиент
     */
    @GetMapping("/api/clients/{id}")
    ClientDto getClient(@PathVariable Long id, @RequestParam Long masterId);

    /**
     * Обновить клиента.
     *
     * @param id        ID клиента
     * @param masterId  ID мастера
     * @param clientDto данные для обновления
     * @return обновленный клиент
     */
    @PutMapping("/api/clients/{id}")
    ClientDto updateClient(@PathVariable Long id,
                           @RequestParam("masterId") Long masterId,
                           @RequestBody ClientDto clientDto);

    /**
     * Удалить клиента.
     *
     * @param id       ID клиента
     * @param masterId ID мастера
     */
    @DeleteMapping("/api/clients/{id}")
    void deleteClient(@PathVariable Long id,
                      @RequestParam("masterId") Long masterId);

    /**
     * Получить всех клиентов мастера (без пагинации).
     *
     * @param masterId ID мастера
     * @return список клиентов
     */
    @GetMapping("/api/clients/all")
    List<ClientDto> getAllClients(@RequestParam("masterId") Long masterId);

    /**
     * Получить клиента по email.
     *
     * @param email email клиента
     * @return клиент
     */
    @GetMapping("/api/clients/by-email")
    ClientDto getClientByEmail(@RequestParam("email") String email);

    /**
     * Обновить профиль клиента.
     *
     * @param id        ID клиента
     * @param clientDto данные для обновления
     * @return обновленный клиент
     */
    @PutMapping("/api/clients/{id}/profile")
    ClientDto updateClientProfile(@PathVariable Long id, @RequestBody ClientDto clientDto);

    /**
     * Получить всех клиентов для администратора.
     *
     * @param page    номер страницы
     * @param size    размер страницы
     * @param sortBy  поле для сортировки
     * @param sortDir направление сортировки
     * @return страница с клиентами
     */
    @GetMapping("/api/clients/admin/all")
    Page<ClientDto> getAllClientsForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir
    );

    // ==================== УСЛУГИ ====================

    /**
     * Получить услуги мастера с пагинацией.
     *
     * @param page     номер страницы
     * @param size     размер страницы
     * @param sortBy   поле для сортировки
     * @param sortDir  направление сортировки
     * @param masterId ID мастера
     * @return страница с услугами
     */
    @GetMapping("/api/services/paginated")
    Page<ServiceEntityDto> getServicesPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("masterId") Long masterId
    );

    /**
     * Получить все услуги мастера.
     *
     * @param masterId ID мастера
     * @return список услуг
     */
    @GetMapping("/api/services")
    List<ServiceEntityDto> getServices(@RequestParam("masterId") Long masterId);

    /**
     * Получить услугу по ID.
     *
     * @param id       ID услуги
     * @param masterId ID мастера
     * @return услуга
     */
    @GetMapping("/api/services/{id}")
    ServiceEntityDto getService(@PathVariable Long id,
                                @RequestParam("masterId") Long masterId);

    /**
     * Создать услугу.
     *
     * @param masterId         ID мастера
     * @param serviceEntityDto данные услуги
     * @return созданная услуга
     */
    @PostMapping("/api/services")
    ServiceEntityDto createService(@RequestParam("masterId") Long masterId,
                                   @RequestBody ServiceEntityDto serviceEntityDto);

    /**
     * Обновить услугу.
     *
     * @param id               ID услуги
     * @param masterId         ID мастера
     * @param serviceEntityDto данные для обновления
     * @return обновленная услуга
     */
    @PutMapping("/api/services/{id}")
    ServiceEntityDto updateService(@PathVariable Long id,
                                   @RequestParam("masterId") Long masterId,
                                   @RequestBody ServiceEntityDto serviceEntityDto);

    /**
     * Удалить услугу.
     *
     * @param id ID услуги
     */
    @DeleteMapping("/api/services/{id}")
    void deleteService(@PathVariable Long id);

    /**
     * Получить все услуги мастера (без пагинации).
     *
     * @param masterId ID мастера
     * @return список услуг
     */
    @GetMapping("/api/services/all")
    List<ServiceEntityDto> getAllServices(@RequestParam("masterId") Long masterId);

    /**
     * Получить услуги мастера по ID мастера.
     *
     * @param masterId ID мастера
     * @return список услуг
     */
    @GetMapping("/api/services/by-master/{masterId}")
    List<ServiceEntityDto> getServicesByMasterId(@PathVariable Long masterId);

    /**
     * Получить все услуги для администратора.
     *
     * @param page    номер страницы
     * @param size    размер страницы
     * @param sortBy  поле для сортировки
     * @param sortDir направление сортировки
     * @return страница с услугами
     */
    @GetMapping("/api/services/admin/all")
    Page<ServiceEntityDto> getAllServicesForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir
    );

    /**
     * Получить услугу по ID (без проверки мастера).
     *
     * @param id ID услуги
     * @return услуга
     */
    @GetMapping("/api/services/{id}")
    ServiceEntityDto getServiceById(@PathVariable Long id);

    /**
     * Деактивировать услугу.
     *
     * @param id ID услуги
     */
    @PostMapping("/api/services/{id}/deactivate")
    void deactivateService(@PathVariable Long id);

    /**
     * Активировать услугу.
     *
     * @param id ID услуги
     */
    @PostMapping("/api/services/{id}/activate")
    void activateService(@PathVariable Long id);

    /**
     * Получить материалы для услуги.
     *
     * @param id ID услуги
     * @return список материалов услуги
     */
    @GetMapping("/api/services/{id}/materials")
    List<ServiceMaterialDto> getServiceMaterials(@PathVariable Long id);

    /**
     * Добавить материал к услуге.
     *
     * @param id           ID услуги
     * @param materialId   ID материала
     * @param quantityUsed количество
     * @param notes        примечание
     */
    @PostMapping("/api/services/{id}/materials")
    void addMaterialToService(@PathVariable Long id,
                              @RequestParam Long materialId,
                              @RequestParam BigDecimal quantityUsed,
                              @RequestParam(required = false) String notes);

    /**
     * Удалить материал из услуги.
     *
     * @param id ID связи услуги и материала
     */
    @DeleteMapping("/api/service-materials/{id}")
    void removeMaterialFromService(@PathVariable Long id);

    // ==================== МАТЕРИАЛЫ ====================

    /**
     * Получить материалы мастера с пагинацией.
     *
     * @param page     номер страницы
     * @param size     размер страницы
     * @param sortBy   поле для сортировки
     * @param sortDir  направление сортировки
     * @param masterId ID мастера
     * @return страница с материалами
     */
    @GetMapping("/api/materials/paginated")
    Page<MaterialDto> getMaterialsPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("masterId") Long masterId
    );

    /**
     * Получить материал по ID.
     *
     * @param id       ID материала
     * @param masterId ID мастера
     * @return материал
     */
    @GetMapping("/api/materials/{id}")
    MaterialDto getMaterial(@PathVariable Long id,
                            @RequestParam("masterId") Long masterId);

    /**
     * Создать материал.
     *
     * @param masterId    ID мастера
     * @param materialDto данные материала
     * @return созданный материал
     */
    @PostMapping("/api/materials")
    MaterialDto createMaterial(@RequestParam("masterId") Long masterId,
                               @RequestBody MaterialDto materialDto);

    /**
     * Обновить материал.
     *
     * @param id          ID материала
     * @param masterId    ID мастера
     * @param materialDto данные для обновления
     * @return обновленный материал
     */
    @PutMapping("/api/materials/{id}")
    MaterialDto updateMaterial(@PathVariable Long id,
                               @RequestParam("masterId") Long masterId,
                               @RequestBody MaterialDto materialDto);

    /**
     * Удалить материал.
     *
     * @param id ID материала
     */
    @DeleteMapping("/api/materials/{id}")
    void deleteMaterial(@PathVariable Long id);

    /**
     * Получить все материалы мастера.
     *
     * @param masterId ID мастера
     * @return список материалов
     */
    @GetMapping("/api/materials/all")
    List<MaterialDto> getAllMaterials(@RequestParam Long masterId);

    /**
     * Получить все материалы для администратора.
     *
     * @param page    номер страницы
     * @param size    размер страницы
     * @param sortBy  поле для сортировки
     * @param sortDir направление сортировки
     * @return страница с материалами
     */
    @GetMapping("/api/materials/admin/all")
    Page<MaterialDto> getAllMaterialsForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir
    );

    // ==================== ЗАПИСИ ====================

    /**
     * Получить записи мастера с пагинацией.
     *
     * @param page     номер страницы
     * @param size     размер страницы
     * @param sortBy   поле для сортировки
     * @param sortDir  направление сортировки
     * @param masterId ID мастера
     * @return страница с записями
     */
    @GetMapping("/api/appointments/paginated")
    Page<AppointmentDto> getAppointmentsPaginated(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir,
            @RequestParam("masterId") Long masterId
    );

    /**
     * Получить запись по ID.
     *
     * @param id ID записи
     * @return запись
     */
    @GetMapping("/api/appointments/{id}")
    AppointmentDto getAppointment(@PathVariable Long id);

    /**
     * Создать запись.
     *
     * @param createDto данные для создания
     * @return созданная запись
     */
    @PostMapping("/api/appointments")
    AppointmentDto createAppointment(@RequestBody AppointmentCreateDto createDto);

    /**
     * Обновить статус записи.
     *
     * @param id     ID записи
     * @param status новый статус
     * @return обновленная запись
     */
    @PostMapping("/api/appointments/{id}/status")
    AppointmentDto updateAppointmentStatus(@PathVariable Long id, @RequestParam("status") String status);

    /**
     * Удалить запись.
     *
     * @param id ID записи
     */
    @DeleteMapping("/api/appointments/{id}")
    void deleteAppointment(@PathVariable Long id);

    /**
     * Обновить запись.
     *
     * @param id        ID записи
     * @param createDto данные для обновления
     * @return обновленная запись
     */
    @PutMapping("/api/appointments/{id}")
    AppointmentDto updateAppointment(@PathVariable Long id, @RequestBody AppointmentCreateDto createDto);

    /**
     * Получить записи клиента.
     *
     * @param clientId ID клиента
     * @return список записей
     */
    @GetMapping("/api/appointments/by-client/{clientId}")
    List<AppointmentDto> getAppointmentsByClientId(@PathVariable Long clientId);

    /**
     * Получить записи мастера за период.
     *
     * @param startDate начальная дата
     * @param endDate   конечная дата
     * @param masterId  ID мастера
     * @return список записей
     */
    @GetMapping("/api/appointments/calendar")
    List<AppointmentDto> getAppointmentsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("masterId") Long masterId
    );

    /**
     * Проверить доступность слота.
     *
     * @param masterId  ID мастера
     * @param date      дата
     * @param startTime время начала
     * @param endTime   время окончания
     * @return true если доступно
     */
    @GetMapping("/api/appointments/check-availability")
    boolean checkAvailability(
            @RequestParam("masterId") Long masterId,
            @RequestParam("date") String date,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime
    );

    /**
     * Получить все записи мастера.
     *
     * @param masterId ID мастера
     * @return список записей
     */
    @GetMapping("/api/appointments/by-master/{masterId}")
    List<AppointmentDto> getAppointmentsByMasterId(@PathVariable Long masterId);

    /**
     * Получить все записи для администратора.
     *
     * @param page    номер страницы
     * @param size    размер страницы
     * @param sortBy  поле для сортировки
     * @param sortDir направление сортировки
     * @return страница с записями
     */
    @GetMapping("/api/appointments/admin/all")
    Page<AppointmentDto> getAllAppointmentsForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("sortBy") String sortBy,
            @RequestParam("sortDir") String sortDir
    );

    // ==================== СЛОТЫ ДОСТУПНОСТИ ====================

    /**
     * Получить свободные слоты.
     *
     * @param masterId  ID мастера
     * @param serviceId ID услуги
     * @param date      дата
     * @return список свободных слотов
     */
    @GetMapping("/api/availability/slots")
    List<AvailabilitySlotDto> getFreeSlots(
            @RequestParam("masterId") Long masterId,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam("date") String date
    );

    /**
     * Создать слот доступности.
     *
     * @param slotDto данные слота
     * @return созданный слот
     */
    @PostMapping("/api/availability/slots")
    AvailabilitySlotDto createSlot(@RequestBody AvailabilitySlotDto slotDto);

    /**
     * Удалить слот.
     *
     * @param slotId ID слота
     */
    @DeleteMapping("/api/availability/slots/{slotId}")
    void deleteSlot(@PathVariable Long slotId);

    /**
     * Получить свободные слоты за период.
     *
     * @param masterId  ID мастера
     * @param serviceId ID услуги
     * @param startDate начальная дата
     * @param endDate   конечная дата
     * @return список слотов
     */
    @GetMapping("/api/availability/slots-by-date-range")
    List<AvailabilitySlotDto> getFreeSlotsByDateRange(
            @RequestParam("masterId") Long masterId,
            @RequestParam("serviceId") Long serviceId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );

    /**
     * Получить слоты по дате.
     *
     * @param date дата
     * @return список слотов
     */
    @GetMapping("/api/availability/slots/by-date")
    List<AvailabilitySlotDto> getSlotsByDate(@RequestParam("date") String date);

    /**
     * Получить все слоты мастера.
     *
     * @param masterId ID мастера
     * @return список слотов
     */
    @GetMapping("/api/availability/slots/all")
    List<AvailabilitySlotDto> getAllSlots(@RequestParam Long masterId);

    /**
     * Забронировать слот.
     *
     * @param slotId ID слота
     */
    @PostMapping("/api/availability/slots/{slotId}/book")
    void bookSlot(@PathVariable Long slotId);

    /**
     * Освободить слот.
     *
     * @param masterId  ID мастера
     * @param date      дата
     * @param startTime время начала
     */
    @PostMapping("/api/availability/slots/release")
    void releaseSlot(@RequestParam Long masterId, @RequestParam String date, @RequestParam String startTime);

    // ==================== МАСТЕРА ====================

    /**
     * Получить мастера по email.
     *
     * @param email email мастера
     * @return мастер
     */
    @GetMapping("/api/masters/by-email")
    MasterDto getMasterByEmail(@RequestParam("email") String email);

    /**
     * Получить мастера по ID.
     *
     * @param id ID мастера
     * @return мастер
     */
    @GetMapping("/api/masters/{id}")
    MasterDto getMasterById(@PathVariable Long id);

    /**
     * Получить профиль мастера.
     *
     * @param id ID мастера
     * @return профиль
     */
    @GetMapping("/api/masters/profile/{id}")
    MasterDto getMasterProfile(@PathVariable Long id);

    /**
     * Обновить профиль мастера.
     *
     * @param id        ID мастера
     * @param updateDto данные для обновления
     * @return обновленный профиль
     */
    @PutMapping("/api/masters/profile/{id}")
    MasterDto updateMasterProfile(@PathVariable Long id, @RequestBody MasterUpdateDto updateDto);

    /**
     * Получить всех мастеров.
     *
     * @return список мастеров
     */
    @GetMapping("/api/masters")
    List<MasterDto> getAllMasters();
}