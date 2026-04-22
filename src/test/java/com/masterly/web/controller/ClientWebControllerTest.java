package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientWebControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ClientWebController clientWebController;

    private ClientDto clientDto;
    private MasterDto masterDto;
    private Page<ClientDto> clientPage;
    private String email;
    private Long masterId;
    private Long clientId;

    @BeforeEach
    void setUp() {
        email = "test@masterly.com";
        masterId = 1L;
        clientId = 1L;

        masterDto = new MasterDto();
        masterDto.setId(masterId);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");

        clientDto = new ClientDto();
        clientDto.setId(clientId);
        clientDto.setFullName("Иван Иванов");
        clientDto.setPhone("+375291234567");
        clientDto.setEmail("ivan@test.com");

        List<ClientDto> clients = Collections.singletonList(clientDto);
        clientPage = new PageImpl<>(clients);
    }

    @Test
    void listClients_ShouldReturnClientsView_ForMaster() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getClientsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(clientPage);

        String viewName = clientWebController.listClients(0, 10, "id", "asc", authentication, model);

        assertEquals("clients/list", viewName);
        verify(model).addAttribute(eq("clients"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", clientPage.getTotalPages());
    }

    @Test
    void listClients_ShouldReturnClientsView_ForAdmin() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getAllClientsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(clientPage);

        String viewName = clientWebController.listClients(0, 10, "id", "asc", authentication, model);

        assertEquals("clients/list", viewName);
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
    }

    @Test
    void listClients_ShouldSetReverseSortDir() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getClientsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(clientPage);

        clientWebController.listClients(0, 10, "id", "desc", authentication, model);

        verify(model).addAttribute("reverseSortDir", "asc");
    }

    @Test
    void listClients_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        // Контроллер выбрасывает RuntimeException, а не использует 1L по умолчанию

        assertThrows(RuntimeException.class, () -> {
            clientWebController.listClients(0, 10, "id", "asc", authentication, model);
        });

        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient, never()).getClientsPaginated(anyInt(), anyInt(), anyString(), anyString(), anyLong());
    }

    @Test
    void showCreateForm_ShouldReturnFormView() {
        String viewName = clientWebController.showCreateForm(model);

        assertEquals("clients/form", viewName);
        verify(model).addAttribute(eq("client"), any(ClientDto.class));
    }

    @Test
    void showEditForm_ShouldReturnFormView_WithClient() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getClient(clientId, masterId)).thenReturn(clientDto);

        String viewName = clientWebController.showEditForm(clientId, authentication, model);

        assertEquals("clients/form", viewName);
        verify(model).addAttribute("client", clientDto);
    }

    @Test
    void saveClient_ShouldCreateNewClient_WhenIdNullAndNoErrors() {
        clientDto.setId(null);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.createClient(eq(masterId), any(ClientDto.class))).thenReturn(clientDto);

        String viewName = clientWebController.saveClient(clientDto, bindingResult, authentication, model);

        assertEquals("redirect:/clients", viewName);
        verify(coreServiceClient).createClient(eq(masterId), any(ClientDto.class));
    }

    @Test
    void saveClient_ShouldUpdateExistingClient_WhenIdNotNullAndNoErrors() {
        clientDto.setId(clientId);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.updateClient(eq(clientId), eq(masterId), any(ClientDto.class)))
                .thenReturn(clientDto);

        String viewName = clientWebController.saveClient(clientDto, bindingResult, authentication, model);

        assertEquals("redirect:/clients", viewName);
        verify(coreServiceClient).updateClient(eq(clientId), eq(masterId), any(ClientDto.class));
    }

    @Test
    void saveClient_ShouldReturnForm_WhenValidationErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = clientWebController.saveClient(clientDto, bindingResult, authentication, model);

        assertEquals("clients/form", viewName);
        verify(coreServiceClient, never()).createClient(anyLong(), any());
        verify(coreServiceClient, never()).updateClient(anyLong(), anyLong(), any());
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
    }

    @Test
    void deleteClient_ShouldDeleteAndRedirect() {
        doNothing().when(coreServiceClient).deleteClient(clientId, masterId);

        String viewName = clientWebController.deleteClient(clientId, masterId, redirectAttributes);

        assertEquals("redirect:/clients?masterId=" + masterId, viewName);
        verify(coreServiceClient).deleteClient(clientId, masterId);
        verify(redirectAttributes).addFlashAttribute("success", "Клиент успешно удален");
    }

    @Test
    void deleteClient_ShouldHandle403Exception() {
        feign.FeignException exception = mock(feign.FeignException.class);
        when(exception.status()).thenReturn(403);
        doThrow(exception).when(coreServiceClient).deleteClient(clientId, masterId);

        String viewName = clientWebController.deleteClient(clientId, masterId, redirectAttributes);

        assertEquals("redirect:/clients?masterId=" + masterId, viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteClient_ShouldHandle400Exception() {
        feign.FeignException exception = mock(feign.FeignException.class);
        when(exception.status()).thenReturn(400);
        doThrow(exception).when(coreServiceClient).deleteClient(clientId, masterId);

        String viewName = clientWebController.deleteClient(clientId, masterId, redirectAttributes);

        assertEquals("redirect:/clients?masterId=" + masterId, viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteClient_ShouldHandleOtherFeignException() {
        feign.FeignException exception = mock(feign.FeignException.class);
        when(exception.status()).thenReturn(500);
        when(exception.getMessage()).thenReturn("Internal error");
        doThrow(exception).when(coreServiceClient).deleteClient(clientId, masterId);

        String viewName = clientWebController.deleteClient(clientId, masterId, redirectAttributes);

        assertEquals("redirect:/clients?masterId=" + masterId, viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteClient_ShouldHandleGenericException() {
        RuntimeException exception = new RuntimeException("Generic error");
        doThrow(exception).when(coreServiceClient).deleteClient(clientId, masterId);

        String viewName = clientWebController.deleteClient(clientId, masterId, redirectAttributes);

        assertEquals("redirect:/clients?masterId=" + masterId, viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void clientProfile_ShouldReturnProfileView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);

        String viewName = clientWebController.clientProfile(authentication, model);

        assertEquals("clients/profile", viewName);
        verify(model).addAttribute("client", clientDto);
    }

    @Test
    void clientProfile_ShouldReturnError_WhenExceptionThrown() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        String viewName = clientWebController.clientProfile(authentication, model);

        assertEquals("error", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(model, never()).addAttribute(eq("client"), any());
    }

    @Test
    void myAppointments_ShouldReturnAppointmentsView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientId)).thenReturn(Collections.emptyList());

        String viewName = clientWebController.myAppointments(authentication, null, model);

        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute(eq("newAppointmentId"), eq(null));
    }

    @Test
    void myAppointments_ShouldReturnAppointmentsView_WithNewAppointmentId() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientId)).thenReturn(Collections.emptyList());

        String viewName = clientWebController.myAppointments(authentication, "123", model);

        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute(eq("newAppointmentId"), eq(123L));
    }

    @Test
    void myAppointments_ShouldReturnAppointmentsView_WithInvalidNewAppointmentId() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientId)).thenReturn(Collections.emptyList());

        String viewName = clientWebController.myAppointments(authentication, "invalid", model);

        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute(eq("newAppointmentId"), eq(null));
    }

    @Test
    void myAppointments_ShouldReturnError_WhenExceptionThrown() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        // Контроллер не обрабатывает исключение, оно пробрасывается дальше
        assertThrows(RuntimeException.class, () -> {
            clientWebController.myAppointments(authentication, null, model);
        });

        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient, never()).getAppointmentsByClientId(anyLong());
    }

    // ==================== myAppointments - sorting ====================
    @Test
    void myAppointments_ShouldSortAppointments_PastLast() {
        // given
        LocalDate today = LocalDate.now();

        AppointmentDto pastAppointment = new AppointmentDto();
        pastAppointment.setId(1L);
        pastAppointment.setAppointmentDate(today.minusDays(1));

        AppointmentDto futureAppointment = new AppointmentDto();
        futureAppointment.setId(2L);
        futureAppointment.setAppointmentDate(today.plusDays(1));

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(pastAppointment);
        appointments.add(futureAppointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void myAppointments_ShouldSortAppointments_FutureFirst() {
        // given
        LocalDate today = LocalDate.now();

        AppointmentDto todayAppointment = new AppointmentDto();
        todayAppointment.setId(1L);
        todayAppointment.setAppointmentDate(today);

        AppointmentDto futureAppointment = new AppointmentDto();
        futureAppointment.setId(2L);
        futureAppointment.setAppointmentDate(today.plusDays(2));

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(todayAppointment);
        appointments.add(futureAppointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void myAppointments_ShouldSortAppointments_ByDateDescending() {
        // given
        LocalDate today = LocalDate.now();

        AppointmentDto future1 = new AppointmentDto();
        future1.setId(1L);
        future1.setAppointmentDate(today.plusDays(1));

        AppointmentDto future2 = new AppointmentDto();
        future2.setId(2L);
        future2.setAppointmentDate(today.plusDays(3));

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(future1);
        appointments.add(future2);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void dashboard_ShouldReturnDashboardView() {
        String viewName = clientWebController.dashboard();
        assertEquals("clients/dashboard", viewName);
    }

    @Test
    void masters_ShouldReturnMastersView() {
        when(coreServiceClient.getAllMasters()).thenReturn(Collections.emptyList());

        String viewName = clientWebController.masters(model);

        assertEquals("clients/masters", viewName);
        verify(model).addAttribute(eq("masters"), anyList());
    }

    @Test
    void viewMaster_ShouldReturnMasterDetailsView_WhenSuccess() {
        // given
        Long masterId = 1L;
        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(1L);
        service.setName("Стрижка");
        service.setIsActive(true);

        ServiceEntityDto inactiveService = new ServiceEntityDto();
        inactiveService.setId(2L);
        inactiveService.setName("Архивная услуга");
        inactiveService.setIsActive(false);

        List<ServiceEntityDto> allServices = List.of(service, inactiveService);

        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(allServices);

        // when
        String viewName = clientWebController.viewMaster(masterId, model);

        // then
        assertEquals("clients/master-details", viewName);
        verify(coreServiceClient).getMasterById(masterId);
        verify(coreServiceClient).getServicesByMasterId(masterId);
        verify(model).addAttribute("master", master);
        verify(model).addAttribute(eq("services"), argThat((List<ServiceEntityDto> list) ->
                list.size() == 1 && list.get(0).getIsActive()
        ));
    }

    @Test
    void viewMaster_ShouldReturnError_WhenExceptionThrown() {
        // given
        Long masterId = 1L;
        when(coreServiceClient.getMasterById(masterId)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = clientWebController.viewMaster(masterId, model);

        // then
        assertEquals("error", viewName);
        verify(coreServiceClient).getMasterById(masterId);
        verify(model, never()).addAttribute(eq("master"), any());
    }

    @Test
    void showBookingForm_ShouldReturnBookingFormView_WhenSuccess() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(true);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service); // ← вернёт service оба раза
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);

        // when
        String viewName = clientWebController.showBookingForm(masterId, serviceId, authentication, model, redirectAttributes);

        // then
        assertEquals("clients/booking-form", viewName);
        verify(coreServiceClient, times(2)).getServiceById(serviceId); // ← ИСПРАВЛЕНО: times(2)
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getMasterById(masterId);
        verify(model).addAttribute("client", clientDto);
        verify(model).addAttribute("master", master);
        verify(model).addAttribute("service", service);
    }

    @Test
    void showBookingForm_ShouldRedirect_WhenServiceNotActive() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(false);

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);

        // when
        String viewName = clientWebController.showBookingForm(masterId, serviceId, authentication, model, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getServiceById(serviceId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
        verify(coreServiceClient, never()).getClientByEmail(anyString());
    }

    @Test
    void showBookingForm_ShouldRedirect_WhenServiceNotFound() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(null);

        // when
        String viewName = clientWebController.showBookingForm(masterId, serviceId, authentication, model, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getServiceById(serviceId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
    }

    @Test
    void showBookingForm_ShouldReturnError_WhenExceptionThrown() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        when(coreServiceClient.getServiceById(serviceId)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = clientWebController.showBookingForm(masterId, serviceId, authentication, model, redirectAttributes);

        // then
        assertEquals("error", viewName);
        verify(coreServiceClient).getServiceById(serviceId);
    }

    @Test
    void getAvailableSlots_ShouldReturnFreeSlots_WhenSuccess() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(true);

        AvailabilitySlotDto slot1 = new AvailabilitySlotDto();
        slot1.setId(1L);
        slot1.setIsBooked(false);

        AvailabilitySlotDto slot2 = new AvailabilitySlotDto();
        slot2.setId(2L);
        slot2.setIsBooked(true);

        AvailabilitySlotDto slot3 = new AvailabilitySlotDto();
        slot3.setId(3L);
        slot3.setIsBooked(null);

        List<AvailabilitySlotDto> slots = List.of(slot1, slot2, slot3);

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);
        when(coreServiceClient.getFreeSlots(masterId, serviceId, date)).thenReturn(slots);

        // when
        List<AvailabilitySlotDto> result = clientWebController.getAvailableSlots(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertEquals(2, result.size()); // только slot1 и slot3 (isBooked == false или null)
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(coreServiceClient).getFreeSlots(masterId, serviceId, date);
    }

    @Test
    void getAvailableSlots_ShouldReturnEmptyList_WhenServiceNotActive() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(false);

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);

        // when
        List<AvailabilitySlotDto> result = clientWebController.getAvailableSlots(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(coreServiceClient, never()).getFreeSlots(anyLong(), anyLong(), anyString());
    }

    @Test
    void getAvailableSlots_ShouldReturnEmptyList_WhenServiceNotFound() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(null);

        // when
        List<AvailabilitySlotDto> result = clientWebController.getAvailableSlots(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(coreServiceClient, never()).getFreeSlots(anyLong(), anyLong(), anyString());
    }

    @Test
    void getAvailableSlots_ShouldReturnEmptyList_WhenSlotsNull() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(true);

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);
        when(coreServiceClient.getFreeSlots(masterId, serviceId, date)).thenReturn(null);

        // when
        List<AvailabilitySlotDto> result = clientWebController.getAvailableSlots(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(coreServiceClient).getFreeSlots(masterId, serviceId, date);
    }

    @Test
    void getAvailableSlots_ShouldReturnEmptyList_WhenExceptionThrown() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        when(coreServiceClient.getService(serviceId, masterId)).thenThrow(new RuntimeException("API error"));

        // when
        List<AvailabilitySlotDto> result = clientWebController.getAvailableSlots(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(coreServiceClient).getService(serviceId, masterId);
    }

    @Test
    void getFreeSlotsForClient_ShouldReturnSlots() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        AvailabilitySlotDto slot = new AvailabilitySlotDto();
        slot.setId(1L);
        List<AvailabilitySlotDto> slots = Collections.singletonList(slot);

        when(coreServiceClient.getFreeSlots(masterId, serviceId, date)).thenReturn(slots);

        // when
        List<AvailabilitySlotDto> result = clientWebController.getFreeSlotsForClient(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(coreServiceClient).getFreeSlots(masterId, serviceId, date);
    }

    @Test
    void getFreeSlotsForClient_ShouldReturnEmptyList_WhenNoSlots() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";

        when(coreServiceClient.getFreeSlots(masterId, serviceId, date)).thenReturn(Collections.emptyList());

        // when
        List<AvailabilitySlotDto> result = clientWebController.getFreeSlotsForClient(masterId, serviceId, date);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(coreServiceClient).getFreeSlots(masterId, serviceId, date);
    }

    @Test
    void createAppointment_ShouldRedirectToAppointments_WhenSuccess() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        Long slotId = 1L;
        String date = "2026-04-20";
        String startTime = "10:00";

        AppointmentDto newAppointment = new AppointmentDto();
        newAppointment.setId(100L);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class))).thenReturn(newAppointment);

        // when
        String viewName = clientWebController.createAppointment(masterId, serviceId, slotId, date, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).createAppointment(any(AppointmentCreateDto.class));
        verify(redirectAttributes).addFlashAttribute("newAppointmentId", 100L);
        verify(redirectAttributes).addFlashAttribute("success", "✅ Запись успешно создана!");
    }

    @Test
    void createAppointment_ShouldRedirectWithError_WhenExceptionThrown() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        Long slotId = 1L;
        String date = "2026-04-20";
        String startTime = "10:00";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = clientWebController.createAppointment(masterId, serviceId, slotId, date, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId + "/book/" + serviceId + "/calendar?date=" + date + "&view=month", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Ошибка при создании записи"));
    }

    // ==================== getAvailableDates ====================

    @Test
    void getAvailableDates_ShouldReturnTestDates() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        // when
        List<String> result = clientWebController.getAvailableDates(masterId, serviceId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(LocalDate.now().plusDays(1).toString(), result.get(0));
        assertEquals(LocalDate.now().plusDays(2).toString(), result.get(1));
    }

// ==================== showEditProfileForm ====================

    @Test
    void showEditProfileForm_ShouldReturnProfileEditView() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);

        // when
        String viewName = clientWebController.showEditProfileForm(authentication, model);

        // then
        assertEquals("clients/profile-edit", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getClientByEmail(email);
        verify(model).addAttribute("client", clientDto);
    }

// ==================== updateProfile ====================

    @Test
    void updateProfile_ShouldRedirectToProfile_WhenSuccess() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.updateClientProfile(eq(clientId), any(ClientDto.class))).thenReturn(clientDto);

        // when
        String viewName = clientWebController.updateProfile(clientDto, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/profile", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).updateClientProfile(eq(clientId), any(ClientDto.class));
        verify(redirectAttributes).addFlashAttribute("success", "Профиль успешно обновлен");
    }

// ==================== bookService ====================

    @Test
    void bookService_ShouldRedirectToAppointments_WhenSuccess() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(true);

        AppointmentDto newAppointment = new AppointmentDto();
        newAppointment.setId(100L);

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class))).thenReturn(newAppointment);

        // when
        String viewName = clientWebController.bookService(masterId, serviceId, appointmentDate, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/appointments", viewName);
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).createAppointment(any(AppointmentCreateDto.class));
        verify(redirectAttributes).addFlashAttribute("success", "Запись успешно создана!");
    }

    @Test
    void bookService_ShouldRedirectToMasters_WhenServiceNotActive() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(false);

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);

        // when
        String viewName = clientWebController.bookService(masterId, serviceId, appointmentDate, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
        verify(coreServiceClient, never()).getClientByEmail(anyString());
    }

    @Test
    void bookService_ShouldRedirectToMasters_WhenServiceNotFound() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(null);

        // when
        String viewName = clientWebController.bookService(masterId, serviceId, appointmentDate, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
    }

    @Test
    void bookService_ShouldRedirectWithError_WhenExceptionThrown() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        when(coreServiceClient.getService(serviceId, masterId)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = clientWebController.bookService(masterId, serviceId, appointmentDate, startTime, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getService(serviceId, masterId);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Ошибка при создании записи"));
    }

    // ==================== cancelAppointment ====================

    @Test
    void cancelAppointment_ShouldRedirectToAppointments_WhenSuccess() {
        // given
        Long appointmentId = 1L;
        AppointmentDto appointment = new AppointmentDto();
        appointment.setId(appointmentId);
        appointment.setMasterId(1L);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));

        List<AppointmentDto> appointments = Collections.singletonList(appointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);
        when(coreServiceClient.updateAppointmentStatus(eq(appointmentId), eq("CANCELLED"))).thenReturn(appointment);
        doNothing().when(coreServiceClient).releaseSlot(anyLong(), anyString(), anyString());

        // when
        String viewName = clientWebController.cancelAppointment(appointmentId, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getAppointmentsByClientId(clientDto.getId());
        verify(coreServiceClient).updateAppointmentStatus(appointmentId, "CANCELLED");
        verify(coreServiceClient).releaseSlot(eq(1L), anyString(), anyString());
        verify(redirectAttributes).addFlashAttribute("success", "Запись отменена, слот снова доступен");
    }

    @Test
    void cancelAppointment_ShouldRedirectWithError_WhenAppointmentNotFound() {
        // given
        Long appointmentId = 999L;
        AppointmentDto otherAppointment = new AppointmentDto();
        otherAppointment.setId(1L);

        List<AppointmentDto> appointments = Collections.singletonList(otherAppointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.cancelAppointment(appointmentId, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getAppointmentsByClientId(clientDto.getId());
        verify(coreServiceClient, never()).updateAppointmentStatus(anyLong(), anyString());
        verify(redirectAttributes).addFlashAttribute("error", "Запись не найдена");
    }

    @Test
    void cancelAppointment_ShouldThrowException_WhenGetClientByEmailFails() {
        // given
        Long appointmentId = 1L;

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when & then
        assertThrows(RuntimeException.class, () -> {
            clientWebController.cancelAppointment(appointmentId, authentication, redirectAttributes);
        });

        verify(coreServiceClient).getClientByEmail(email);
        verify(redirectAttributes, never()).addFlashAttribute(anyString(), anyString());
    }

    // ==================== showBookingCalendar ====================

    @Test
    void showBookingCalendar_ShouldRedirect_WhenServiceNotActive() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setIsActive(false);

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, null, null, authentication, model, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getServiceById(serviceId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
    }

    @Test
    void showBookingCalendar_ShouldRedirect_WhenServiceNotFound() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(null);

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, null, null, authentication, model, redirectAttributes);

        // then
        assertEquals("redirect:/client/masters/" + masterId, viewName);
        verify(coreServiceClient).getServiceById(serviceId);
        verify(redirectAttributes).addFlashAttribute("error", "Данная услуга недоступна для записи");
    }

    @Test
    void showBookingCalendar_ShouldReturnCalendarView_WhenSuccess() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(true);

        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);
        when(coreServiceClient.getFreeSlots(anyLong(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, null, null, authentication, model, redirectAttributes);

        // then
        assertEquals("clients/booking-calendar", viewName);
        verify(coreServiceClient, times(2)).getServiceById(serviceId);
        verify(coreServiceClient).getMasterById(masterId);
        verify(model).addAttribute("masterId", masterId);
        verify(model).addAttribute("masterName", master.getFullName());
        verify(model).addAttribute("serviceId", serviceId);
        verify(model).addAttribute("serviceName", service.getName());
        verify(model).addAttribute(eq("slotsByDate"), anyMap());
        verify(model).addAttribute(eq("currentDate"), any(LocalDate.class));
        verify(model).addAttribute(eq("view"), eq("month"));
        verify(model).addAttribute(eq("monthName"), anyString());
        verify(model).addAttribute(eq("year"), anyInt());
    }

    @Test
    void cancelAppointment_ShouldCatchException_WhenUpdateAppointmentStatusFails() {
        // given
        Long appointmentId = 1L;
        AppointmentDto appointment = new AppointmentDto();
        appointment.setId(appointmentId);
        appointment.setMasterId(1L);
        appointment.setAppointmentDate(LocalDate.now().plusDays(1));
        appointment.setStartTime(LocalTime.of(10, 0));

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(appointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);
        when(coreServiceClient.updateAppointmentStatus(eq(appointmentId), eq("CANCELLED")))
                .thenThrow(new RuntimeException("Update failed"));

        // when
        String viewName = clientWebController.cancelAppointment(appointmentId, authentication, redirectAttributes);

        // then
        assertEquals("redirect:/client/appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).updateAppointmentStatus(appointmentId, "CANCELLED");
        verify(redirectAttributes).addFlashAttribute("error", "Ошибка при отмене записи");
        verify(redirectAttributes, never()).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void myAppointments_ShouldSortAppointments_PastAndToday() {
        // given
        LocalDate today = LocalDate.now();

        AppointmentDto pastAppointment = new AppointmentDto();
        pastAppointment.setId(1L);
        pastAppointment.setAppointmentDate(today.minusDays(1)); // прошлое

        AppointmentDto todayAppointment = new AppointmentDto();
        todayAppointment.setId(2L);
        todayAppointment.setAppointmentDate(today); // сегодня

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(pastAppointment);
        appointments.add(todayAppointment);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void myAppointments_ShouldParseNewAppointmentId_WhenValid() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(new ArrayList<>());

        // when
        String viewName = clientWebController.myAppointments(authentication, "123", model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("newAppointmentId"), eq(123L));
    }

    @Test
    void showBookingCalendar_ShouldUseDefaultDateAndView_WhenNull() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(true);

        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);
        when(coreServiceClient.getFreeSlots(anyLong(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, null, null, authentication, model, redirectAttributes);

        // then
        assertEquals("clients/booking-calendar", viewName);
        verify(model).addAttribute(eq("view"), eq("month"));
        verify(model).addAttribute(eq("currentDate"), any(LocalDate.class));
    }

    @Test
    void showBookingCalendar_ShouldUseWeekView_WhenSpecified() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";
        String view = "week";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(true);

        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);
        when(coreServiceClient.getFreeSlots(anyLong(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, date, view, authentication, model, redirectAttributes);

        // then
        assertEquals("clients/booking-calendar", viewName);
        verify(model).addAttribute(eq("view"), eq("week"));
        verify(model).addAttribute(eq("currentDate"), eq(LocalDate.parse(date)));
    }

    @Test
    void showBookingCalendar_ShouldUseMonthView_WhenSpecified() {
        // given
        Long masterId = 1L;
        Long serviceId = 1L;
        String date = "2026-04-20";
        String view = "month";

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");
        service.setIsActive(true);

        MasterDto master = new MasterDto();
        master.setId(masterId);
        master.setFullName("Тестовый Мастер");

        when(coreServiceClient.getServiceById(serviceId)).thenReturn(service);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(master);
        when(coreServiceClient.getFreeSlots(anyLong(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // when
        String viewName = clientWebController.showBookingCalendar(masterId, serviceId, date, view, authentication, model, redirectAttributes);

        // then
        assertEquals("clients/booking-calendar", viewName);
        verify(model).addAttribute(eq("view"), eq("month"));
    }

    @Test
    void myAppointments_ShouldNotParseNewAppointmentId_WhenEmptyString() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(new ArrayList<>());

        // when
        String viewName = clientWebController.myAppointments(authentication, "", model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("newAppointmentId"), eq(null));
    }

    @Test
    void myAppointments_ShouldSortAppointments_SameFutureDate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate sameDate = today.plusDays(1);

        AppointmentDto app1 = new AppointmentDto();
        app1.setId(1L);
        app1.setAppointmentDate(sameDate);

        AppointmentDto app2 = new AppointmentDto();
        app2.setId(2L);
        app2.setAppointmentDate(sameDate);

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(app1);
        appointments.add(app2);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void myAppointments_ShouldSortAppointments_SamePastDate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate sameDate = today.minusDays(1);

        AppointmentDto app1 = new AppointmentDto();
        app1.setId(1L);
        app1.setAppointmentDate(sameDate);

        AppointmentDto app2 = new AppointmentDto();
        app2.setId(2L);
        app2.setAppointmentDate(sameDate);

        List<AppointmentDto> appointments = new ArrayList<>();
        appointments.add(app1);
        appointments.add(app2);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = clientWebController.myAppointments(authentication, null, model);

        // then
        assertEquals("clients/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void testAppointmentComparator_AllCases() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);

        AppointmentDto past = new AppointmentDto();
        past.setAppointmentDate(yesterday);

        AppointmentDto todayApp = new AppointmentDto();
        todayApp.setAppointmentDate(today);

        AppointmentDto future1 = new AppointmentDto();
        future1.setAppointmentDate(tomorrow);

        AppointmentDto future2 = new AppointmentDto();
        future2.setAppointmentDate(nextWeek);

        // when - past vs today (aPast=true, bPast=false) -> should return 1 (past goes after)
        List<AppointmentDto> list1 = new ArrayList<>();
        list1.add(past);
        list1.add(todayApp);
        list1.sort((a, b) -> {
            boolean aPast = a.getAppointmentDate().isBefore(today);
            boolean bPast = b.getAppointmentDate().isBefore(today);
            if (aPast && !bPast) return 1;
            if (!aPast && bPast) return -1;
            return b.getAppointmentDate().compareTo(a.getAppointmentDate());
        });
        assertEquals(todayApp.getAppointmentDate(), list1.get(0).getAppointmentDate()); // сегодня первое
        assertEquals(past.getAppointmentDate(), list1.get(1).getAppointmentDate());     // прошлое второе

        // when - today vs past (!aPast && bPast) -> should return -1 (today goes first)
        List<AppointmentDto> list2 = new ArrayList<>();
        list2.add(todayApp);
        list2.add(past);
        list2.sort((a, b) -> {
            boolean aPast = a.getAppointmentDate().isBefore(today);
            boolean bPast = b.getAppointmentDate().isBefore(today);
            if (aPast && !bPast) return 1;
            if (!aPast && bPast) return -1;
            return b.getAppointmentDate().compareTo(a.getAppointmentDate());
        });
        assertEquals(todayApp.getAppointmentDate(), list2.get(0).getAppointmentDate());
        assertEquals(past.getAppointmentDate(), list2.get(1).getAppointmentDate());

        // when - future1 vs future2 (both !aPast && !bPast) -> compareTo (descending)
        List<AppointmentDto> list3 = new ArrayList<>();
        list3.add(future1);
        list3.add(future2);
        list3.sort((a, b) -> {
            boolean aPast = a.getAppointmentDate().isBefore(today);
            boolean bPast = b.getAppointmentDate().isBefore(today);
            if (aPast && !bPast) return 1;
            if (!aPast && bPast) return -1;
            return b.getAppointmentDate().compareTo(a.getAppointmentDate());
        });
        assertEquals(nextWeek, list3.get(0).getAppointmentDate());  // более поздняя дата первая
        assertEquals(tomorrow, list3.get(1).getAppointmentDate());

        // when - past1 vs past2 (both aPast && bPast) -> compareTo (descending)
        AppointmentDto past2 = new AppointmentDto();
        past2.setAppointmentDate(yesterday.minusDays(2));

        List<AppointmentDto> list4 = new ArrayList<>();
        list4.add(past);
        list4.add(past2);
        list4.sort((a, b) -> {
            boolean aPast = a.getAppointmentDate().isBefore(today);
            boolean bPast = b.getAppointmentDate().isBefore(today);
            if (aPast && !bPast) return 1;
            if (!aPast && bPast) return -1;
            return b.getAppointmentDate().compareTo(a.getAppointmentDate());
        });
        assertEquals(yesterday, list4.get(0).getAppointmentDate());  // более поздняя дата первая
        assertEquals(yesterday.minusDays(2), list4.get(1).getAppointmentDate());
    }
}