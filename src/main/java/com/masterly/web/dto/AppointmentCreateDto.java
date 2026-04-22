package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO для создания записи.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreateDto {
    private Long masterId;
    private Long clientId;
    private Long serviceId;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private String notes;
}