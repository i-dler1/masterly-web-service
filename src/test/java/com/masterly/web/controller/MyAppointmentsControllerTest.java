package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.AppointmentDto;
import com.masterly.web.dto.ClientDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyAppointmentsControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private MyAppointmentsController myAppointmentsController;

    private ClientDto clientDto;
    private AppointmentDto appointmentDto;
    private String email;

    @BeforeEach
    void setUp() {
        email = "client@test.com";

        clientDto = new ClientDto();
        clientDto.setId(1L);
        clientDto.setEmail(email);
        clientDto.setFullName("Тестовый Клиент");

        appointmentDto = new AppointmentDto();
        appointmentDto.setId(1L);
        appointmentDto.setServiceName("Стрижка");
    }

    @Test
    void myAppointments_ShouldReturnMyAppointmentsView_WhenSuccess() {
        // given
        List<AppointmentDto> appointments = Collections.singletonList(appointmentDto);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(appointments);

        // when
        String viewName = myAppointmentsController.myAppointments(authentication, model);

        // then
        assertEquals("my-appointments", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getAppointmentsByClientId(clientDto.getId());
        verify(model).addAttribute("appointments", appointments);
        verify(model, never()).addAttribute(eq("error"), any());
    }

    @Test
    void myAppointments_ShouldReturnViewWithEmptyList_WhenNoAppointments() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId())).thenReturn(Collections.emptyList());

        // when
        String viewName = myAppointmentsController.myAppointments(authentication, model);

        // then
        assertEquals("my-appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getAppointmentsByClientId(clientDto.getId());
        verify(model).addAttribute(eq("appointments"), anyList());
    }

    @Test
    void myAppointments_ShouldAddErrorAttribute_WhenExceptionThrown() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = myAppointmentsController.myAppointments(authentication, model);

        // then
        assertEquals("my-appointments", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient, never()).getAppointmentsByClientId(any());
        verify(model).addAttribute(eq("error"), eq("Ошибка загрузки записей"));
    }

    @Test
    void myAppointments_ShouldAddErrorAttribute_WhenGetAppointmentsThrowsException() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getClientByEmail(email)).thenReturn(clientDto);
        when(coreServiceClient.getAppointmentsByClientId(clientDto.getId()))
                .thenThrow(new RuntimeException("API error"));

        // when
        String viewName = myAppointmentsController.myAppointments(authentication, model);

        // then
        assertEquals("my-appointments", viewName);
        verify(coreServiceClient).getClientByEmail(email);
        verify(coreServiceClient).getAppointmentsByClientId(clientDto.getId());
        verify(model).addAttribute(eq("error"), eq("Ошибка загрузки записей"));
    }
}