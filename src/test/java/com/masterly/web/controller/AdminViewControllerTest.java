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
class AdminViewControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private AdminViewController adminViewController;

    private MasterDto adminDto;
    private ServiceEntityDto serviceEntityDto;
    private MaterialDto materialDto;
    private ClientDto clientDto;
    private AppointmentDto appointmentDto;
    private Page<ServiceEntityDto> servicePage;
    private Page<MaterialDto> materialPage;
    private Page<ClientDto> clientPage;
    private Page<AppointmentDto> appointmentPage;
    private String email;

    @BeforeEach
    void setUp() {
        email = "admin@masterly.com";

        adminDto = new MasterDto();
        adminDto.setId(1L);
        adminDto.setEmail(email);
        adminDto.setFullName("Администратор");
        adminDto.setRole("ADMIN");

        serviceEntityDto = new ServiceEntityDto();
        serviceEntityDto.setId(1L);
        serviceEntityDto.setName("Тестовая услуга");
        serviceEntityDto.setPrice(BigDecimal.valueOf(1000));

        materialDto = new MaterialDto();
        materialDto.setId(1L);
        materialDto.setName("Тестовый материал");

        clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setFullName("Тестовый клиент");

        appointmentDto = new AppointmentDto();
        appointmentDto.setId(1L);
        appointmentDto.setAppointmentDate(LocalDate.now());
        appointmentDto.setStartTime(LocalTime.of(10, 0));
        appointmentDto.setEndTime(LocalTime.of(11, 0));

        servicePage = new PageImpl<>(Collections.singletonList(serviceEntityDto));
        materialPage = new PageImpl<>(Collections.singletonList(materialDto));
        clientPage = new PageImpl<>(Collections.singletonList(clientDto));
        appointmentPage = new PageImpl<>(Collections.singletonList(appointmentDto));
    }

    @Test
    void dashboard_ShouldReturnDashboardView() {
        String viewName = adminViewController.dashboard();
        assertEquals("admin/dashboard", viewName);
    }

    @Test
    void services_ShouldReturnServicesView() {
        when(coreServiceClient.getAllServicesForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(servicePage);

        String viewName = adminViewController.services(0, 10, model);

        assertEquals("admin/services", viewName);
        verify(model).addAttribute(eq("services"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute(eq("totalPages"), any());
    }

    @Test
    void materials_ShouldReturnMaterialsView() {
        when(coreServiceClient.getAllMaterialsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(materialPage);

        String viewName = adminViewController.materials(0, 10, model);

        assertEquals("admin/materials", viewName);
        verify(model).addAttribute(eq("materials"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute(eq("totalPages"), any());
    }

    @Test
    void clients_ShouldReturnClientsView() {
        when(coreServiceClient.getAllClientsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(clientPage);

        String viewName = adminViewController.clients(0, 10, model);

        assertEquals("admin/clients", viewName);
        verify(model).addAttribute(eq("clients"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute(eq("totalPages"), any());
    }

    @Test
    void appointments_ShouldReturnAppointmentsView() {
        when(coreServiceClient.getAllAppointmentsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(appointmentPage);

        String viewName = adminViewController.appointments(0, 10, model);

        assertEquals("admin/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute(eq("totalPages"), any());
    }

    @Test
    void masters_ShouldReturnMastersView() {
        List<MasterDto> masters = Collections.singletonList(adminDto);
        when(coreServiceClient.getAllMasters()).thenReturn(masters);

        String viewName = adminViewController.masters(model);

        assertEquals("admin/masters", viewName);
        verify(model).addAttribute("masters", masters);
    }

    @Test
    void profile_ShouldReturnProfileView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(adminDto);

        String viewName = adminViewController.profile(authentication, model);

        assertEquals("admin/profile", viewName);
        verify(model).addAttribute("admin", adminDto);
    }

    @Test
    void profile_ShouldReturnError_WhenException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));

        String viewName = adminViewController.profile(authentication, model);

        assertEquals("error", viewName);
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model, never()).addAttribute(eq("admin"), any());
    }

    @Test
    void confirmAppointment_ShouldRedirect() {
        Long id = 1L;
        when(coreServiceClient.updateAppointmentStatus(id, "CONFIRMED")).thenReturn(new AppointmentDto());

        String viewName = adminViewController.confirmAppointment(id);

        assertEquals("redirect:/admin/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(id, "CONFIRMED");
    }

    @Test
    void completeAppointment_ShouldRedirect() {
        Long id = 1L;
        when(coreServiceClient.updateAppointmentStatus(id, "COMPLETED")).thenReturn(new AppointmentDto());

        String viewName = adminViewController.completeAppointment(id);

        assertEquals("redirect:/admin/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(id, "COMPLETED");
    }

    @Test
    void cancelAppointment_ShouldRedirect() {
        Long id = 1L;
        when(coreServiceClient.updateAppointmentStatus(id, "CANCELLED")).thenReturn(new AppointmentDto());

        String viewName = adminViewController.cancelAppointment(id);

        assertEquals("redirect:/admin/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(id, "CANCELLED");
    }
}