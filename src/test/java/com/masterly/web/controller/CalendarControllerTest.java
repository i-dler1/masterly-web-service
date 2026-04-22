package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.AppointmentDto;
import com.masterly.web.dto.MasterDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private CalendarController calendarController;

    private List<AppointmentDto> appointments;
    private MasterDto masterDto;
    private String email;

    @BeforeEach
    void setUp() {
        appointments = Collections.emptyList();
        email = "test@masterly.com";

        masterDto = new MasterDto();
        masterDto.setId(1L);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");
    }

    @Test
    void showCalendar_ShouldReturnCalendarView_WithWeekView() {
        // Given
        String date = LocalDate.now().toString();
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When
        String viewName = calendarController.showCalendar(date, view, authentication, model);

        // Then
        assertEquals("appointments/calendar", viewName);
        verify(model).addAttribute(eq("monthName"), anyString());
        verify(model).addAttribute(eq("year"), anyInt());
        verify(model).addAttribute(eq("today"), any(LocalDate.class));
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute(eq("currentDate"), any(LocalDate.class));
        verify(model).addAttribute(eq("startDate"), any(LocalDate.class));
        verify(model).addAttribute(eq("endDate"), any(LocalDate.class));
        verify(model).addAttribute("view", view);
        verify(coreServiceClient).getAppointmentsByDateRange(anyString(), anyString(), eq(1L));
    }

    @Test
    void showCalendar_ShouldReturnCalendarView_WithMonthView() {
        // Given
        String date = LocalDate.now().toString();
        String view = "month";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When
        String viewName = calendarController.showCalendar(date, view, authentication, model);

        // Then
        assertEquals("appointments/calendar", viewName);
        verify(model).addAttribute(eq("offset"), anyInt());
        verify(model).addAttribute(eq("firstDayOfMonth"), any(LocalDate.class));
        verify(model).addAttribute(eq("lastDayOfMonth"), any(LocalDate.class));
        verify(model).addAttribute(eq("daysInRange"), anyList());
        verify(coreServiceClient).getAppointmentsByDateRange(anyString(), anyString(), eq(1L));
    }

    @Test
    void showCalendar_ShouldUseDefaultDate_WhenNotProvided() {
        // Given
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When - используем значение по умолчанию из контроллера
        String defaultDate = LocalDate.now().toString();
        String viewName = calendarController.showCalendar(defaultDate, view, authentication, model);

        // Then
        assertEquals("appointments/calendar", viewName);
        verify(coreServiceClient).getAppointmentsByDateRange(anyString(), anyString(), eq(1L));
    }

    @Test
    void showCalendar_ShouldHandleDifferentDates() {
        // Given
        String date = "2024-12-25";
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When
        String viewName = calendarController.showCalendar(date, view, authentication, model);

        // Then
        assertEquals("appointments/calendar", viewName);
        verify(model).addAttribute(eq("monthName"), eq("Декабрь"));
        verify(model).addAttribute(eq("year"), eq(2024));
    }

    @Test
    void showCalendar_ShouldThrowException_WhenMasterNotFound() {
        // Given
        String date = LocalDate.now().toString();
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("Master not found"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            calendarController.showCalendar(date, view, authentication, model);
        });
        verify(coreServiceClient, never()).getAppointmentsByDateRange(anyString(), anyString(), anyLong());
    }

    @Test
    void showCalendar_WeekView_ShouldCalculateCorrectDateRange() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 18); // Среда
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When
        calendarController.showCalendar(testDate.toString(), view, authentication, model);

        // Then
        // Для среды 18.12.2024 неделя должна начинаться с понедельника 16.12.2024
        verify(coreServiceClient).getAppointmentsByDateRange(
                startsWith("2024-12-16"), // startDate - понедельник
                endsWith("2024-12-22"),   // endDate - воскресенье
                eq(1L)
        );
    }

    @Test
    void showCalendar_MonthView_ShouldCalculateCorrectDateRange() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 12, 15);
        String view = "month";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // When
        calendarController.showCalendar(testDate.toString(), view, authentication, model);

        // Then
        // Декабрь 2024 начинается с воскресенья, поэтому startDate должен быть 25.11.2024 (понедельник)
        verify(coreServiceClient).getAppointmentsByDateRange(
                startsWith("2024-11-25"), // первый понедельник перед 1 декабря
                endsWith("2025-01-05"),   // последнее воскресенье после 31 декабря
                eq(1L)
        );
    }

    @Test
    void showCalendar_ShouldHandleMonthNamesCorrectly() {
        // Given
        String view = "week";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByDateRange(anyString(), anyString(), eq(1L)))
                .thenReturn(appointments);

        // Test all months
        String[] expectedMonths = {
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        };

        for (int month = 1; month <= 12; month++) {
            String date = String.format("2024-%02d-15", month);
            calendarController.showCalendar(date, view, authentication, model);
            verify(model).addAttribute("monthName", expectedMonths[month - 1]);
        }
    }
}