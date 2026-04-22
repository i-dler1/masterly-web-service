package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentCreateControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AppointmentCreateController appointmentCreateController;

    private ClientDto clientDto;
    private MasterDto masterDto;
    private ServiceEntityDto serviceDto;
    private AppointmentCreateDto appointmentCreateDto;
    private AvailabilitySlotDto slotDto;
    private String email;
    private Long masterId;
    private Long serviceId;

    @BeforeEach
    void setUp() {
        email = "client@test.com";
        masterId = 1L;
        serviceId = 1L;

        clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail(email);
        clientDto.setFullName("Тестовый Клиент");

        masterDto = new MasterDto();
        masterDto.setId(masterId);
        masterDto.setFullName("Тестовый Мастер");

        serviceDto = new ServiceEntityDto();
        serviceDto.setId(serviceId);
        serviceDto.setName("Стрижка");

        appointmentCreateDto = AppointmentCreateDto.builder()
                .masterId(masterId)
                .clientId(1L)
                .serviceId(serviceId)
                .appointmentDate(LocalDate.now().plusDays(1))
                .build();

        slotDto = new AvailabilitySlotDto();
        slotDto.setId(1L);
        slotDto.setStartTime(LocalTime.of(10, 0));
        slotDto.setEndTime(LocalTime.of(11, 0));

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    // ==================== showCreateForm ====================

    @Test
    void showCreateForm_ShouldReturnCreateFormView_WhenSuccess() {
        // given
        List<ServiceEntityDto> services = Collections.singletonList(serviceDto);
        List<AvailabilitySlotDto> slots = Collections.singletonList(slotDto);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(services);
        when(coreServiceClient.getFreeSlots(eq(masterId), isNull(), anyString())).thenReturn(slots);

        // when
        String viewName = appointmentCreateController.showCreateForm(masterId, null, authentication, model);

        // then
        assertEquals("appointments/create-form", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getMasterById(masterId);
        verify(coreServiceClient).getServicesByMasterId(masterId);
        verify(model).addAttribute("client", clientDto);
        verify(model).addAttribute("master", masterDto);
        verify(model).addAttribute("services", services);
        verify(model).addAttribute(eq("appointment"), any(AppointmentCreateDto.class));
        verify(model).addAttribute("slots", slots);
    }

    @Test
    void showCreateForm_ShouldPreselectService_WhenServiceIdProvided() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getFreeSlots(eq(masterId), eq(serviceId), anyString())).thenReturn(Collections.emptyList());

        // when
        appointmentCreateController.showCreateForm(masterId, serviceId, authentication, model);

        // then
        verify(coreServiceClient).getFreeSlots(eq(masterId), eq(serviceId), anyString());
    }

    @Test
    void showCreateForm_ShouldReturnError_WhenExceptionThrown() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = appointmentCreateController.showCreateForm(masterId, null, authentication, model);

        // then
        assertEquals("error", viewName);
        verify(model).addAttribute("error", "Ошибка загрузки формы записи");
    }

    // ==================== createAppointment ====================

    @Test
    void createAppointment_ShouldRedirectToMyAppointments_WhenSuccess() {
        // given
        when(coreServiceClient.createAppointment(appointmentCreateDto)).thenReturn(new AppointmentDto());

        // when
        String viewName = appointmentCreateController.createAppointment(appointmentCreateDto, model);

        // then
        assertEquals("redirect:/my-appointments", viewName);
        verify(coreServiceClient).createAppointment(appointmentCreateDto);
    }

    @Test
    void createAppointment_ShouldReturnFormWithError_WhenFeignException() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(409);
        when(coreServiceClient.createAppointment(appointmentCreateDto)).thenThrow(feignException);

        // Настраиваем SecurityContext для повторной загрузки формы
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(Collections.emptyList());

        // when
        String viewName = appointmentCreateController.createAppointment(appointmentCreateDto, model);

        // then
        assertEquals("appointments/create-form", viewName);
        verify(coreServiceClient).createAppointment(appointmentCreateDto);
        verify(model).addAttribute(eq("error"), anyString());
    }

    @Test
    void createAppointment_ShouldReturnError_WhenGenericException() {
        // given
        when(coreServiceClient.createAppointment(appointmentCreateDto)).thenThrow(new RuntimeException("Generic error"));

        // when
        String viewName = appointmentCreateController.createAppointment(appointmentCreateDto, model);

        // then
        assertEquals("error", viewName);
        verify(coreServiceClient).createAppointment(appointmentCreateDto);
        verify(model).addAttribute(eq("error"), contains("Ошибка создания записи"));
    }

    @Test
    void createAppointment_ShouldReturnFormWithError_WhenFeignExceptionNot409() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(500);
        when(feignException.getMessage()).thenReturn("Internal Server Error");
        when(coreServiceClient.createAppointment(appointmentCreateDto)).thenThrow(feignException);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(Collections.emptyList());

        // when
        String viewName = appointmentCreateController.createAppointment(appointmentCreateDto, model);

        // then
        assertEquals("appointments/create-form", viewName);
        verify(coreServiceClient).createAppointment(appointmentCreateDto);
        verify(model).addAttribute(eq("error"), contains("Ошибка создания записи"));
        verify(model).addAttribute("client", clientDto);
        verify(model).addAttribute("master", masterDto);
    }

    @Test
    void createAppointment_ShouldReturnError_WhenReloadFormFails() {
        // given
        FeignException feignException = mock(FeignException.class);
        when(feignException.status()).thenReturn(409);
        when(coreServiceClient.createAppointment(appointmentCreateDto)).thenThrow(feignException);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("Reload failed"));

        // when
        String viewName = appointmentCreateController.createAppointment(appointmentCreateDto, model);

        // then
        assertEquals("error", viewName);
        verify(coreServiceClient).createAppointment(appointmentCreateDto);
        verify(coreServiceClient).getClientByEmail(email);
        verify(model, never()).addAttribute(eq("client"), any());
    }
}