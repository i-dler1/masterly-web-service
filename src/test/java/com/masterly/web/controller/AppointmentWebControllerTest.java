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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentWebControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private AppointmentWebController appointmentWebController;

    private AppointmentDto appointmentDto;
    private MasterDto masterDto;
    private ClientDto clientDto;
    private ServiceEntityDto serviceDto;
    private Page<AppointmentDto> appointmentPage;
    private String email;
    private Long masterId;

    @BeforeEach
    void setUp() {
        email = "test@masterly.com";
        masterId = 1L;

        masterDto = new MasterDto();
        masterDto.setId(masterId);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");

        clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setFullName("Иван Иванов");

        serviceDto = new ServiceEntityDto();
        serviceDto.setId(1L);
        serviceDto.setName("Стрижка");
        serviceDto.setPrice(BigDecimal.valueOf(1000));
        serviceDto.setDurationMinutes(60);

        appointmentDto = new AppointmentDto();
        appointmentDto.setId(1L);
        appointmentDto.setMasterId(masterId);
        appointmentDto.setClientId(1L);
        appointmentDto.setServiceId(1L);
        appointmentDto.setAppointmentDate(LocalDate.now().plusDays(1));
        appointmentDto.setStartTime(LocalTime.of(10, 0));
        appointmentDto.setStatus("PENDING");

        List<AppointmentDto> appointments = Collections.singletonList(appointmentDto);
        appointmentPage = new PageImpl<>(appointments);
    }

    @Test
    void listAppointments_ShouldReturnAppointmentsView_ForMaster() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(appointmentPage);

        String viewName = appointmentWebController.listAppointments(0, 10, "id", "asc", authentication, model);

        assertEquals("appointments/list", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", appointmentPage.getTotalPages());
    }

    @Test
    void listAppointments_ShouldReturnAppointmentsView_ForAdmin() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getAllAppointmentsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(appointmentPage);

        String viewName = appointmentWebController.listAppointments(0, 10, "id", "asc", authentication, model);

        assertEquals("appointments/list", viewName);
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
    }

    @Test
    void listAppointments_ShouldThrowException_WhenGetMasterByEmailFails() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () -> {
            appointmentWebController.listAppointments(0, 10, "id", "asc", authentication, model);
        });

        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient, never()).getAppointmentsPaginated(anyInt(), anyInt(), anyString(), anyString(), anyLong());
    }

    @Test
    void listAppointments_ShouldThrowException_WhenAuthenticationNull() {
        assertThrows(RuntimeException.class, () -> {
            appointmentWebController.listAppointments(0, 10, "id", "asc", null, model);
        });
    }

    @Test
    void listAppointments_ShouldSetReverseSortDir() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(appointmentPage);

        appointmentWebController.listAppointments(0, 10, "id", "desc", authentication, model);

        verify(model).addAttribute("reverseSortDir", "asc");
    }

    @Test
    void deleteAppointment_ShouldDeleteAndRedirect() {
        Long appointmentId = 1L;
        // deleteAppointment возвращает void, поэтому doNothing() ок
        doNothing().when(coreServiceClient).deleteAppointment(appointmentId);

        String viewName = appointmentWebController.deleteAppointment(appointmentId);

        assertEquals("redirect:/appointments", viewName);
        verify(coreServiceClient).deleteAppointment(appointmentId);
    }

    @Test
    void saveAppointment_ShouldCreateNewAppointment_WhenIdNull() {
        AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                .clientId(1L)
                .serviceId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .build();

        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class))).thenReturn(appointmentDto);

        String viewName = appointmentWebController.saveAppointment(createDto, null);

        assertEquals("redirect:/appointments", viewName);
        verify(coreServiceClient).createAppointment(any(AppointmentCreateDto.class));
        verify(coreServiceClient, never()).updateAppointment(anyLong(), any());
    }

    @Test
    void saveAppointment_ShouldUpdateExistingAppointment_WhenIdNotNull() {
        Long appointmentId = 1L;
        AppointmentCreateDto createDto = AppointmentCreateDto.builder()
                .clientId(1L)
                .serviceId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0))
                .build();

        // updateAppointment возвращает AppointmentDto
        when(coreServiceClient.updateAppointment(eq(appointmentId), any(AppointmentCreateDto.class)))
                .thenReturn(appointmentDto);

        String viewName = appointmentWebController.saveAppointment(createDto, appointmentId);

        assertEquals("redirect:/appointments", viewName);
        verify(coreServiceClient).updateAppointment(eq(appointmentId), any(AppointmentCreateDto.class));
        verify(coreServiceClient, never()).createAppointment(any());
    }

    @Test
    void updateStatus_ShouldUpdateAndRedirect() {
        Long appointmentId = 1L;
        String status = "CONFIRMED";
        // updateAppointmentStatus возвращает AppointmentDto
        when(coreServiceClient.updateAppointmentStatus(appointmentId, status))
                .thenReturn(appointmentDto);

        String viewName = appointmentWebController.updateStatus(appointmentId, status);

        assertEquals("redirect:/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(appointmentId, status);
    }

    @Test
    void showEditForm_ShouldReturnFormView() {
        Long appointmentId = 1L;
        when(coreServiceClient.getAppointment(appointmentId)).thenReturn(appointmentDto);
        when(coreServiceClient.getAllClients(eq(1L))).thenReturn(Collections.singletonList(clientDto));
        when(coreServiceClient.getAllServices(eq(1L))).thenReturn(Collections.singletonList(serviceDto));

        String viewName = appointmentWebController.showEditForm(appointmentId, model);

        assertEquals("appointments/form", viewName);
        verify(model).addAttribute(eq("appointment"), any(AppointmentCreateDto.class));
        verify(model).addAttribute("appointmentId", appointmentId);
        verify(model).addAttribute(eq("clients"), anyList());
        verify(model).addAttribute(eq("services"), anyList());
    }
}