package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO клиента.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDto {
    private Long id;
    private Long masterId;
    private String fullName;
    private String phone;
    private String email;
    private String instagram;
    private String telegram;
    private String notes;
    private Boolean isRegular;
    private LocalDateTime lastAppointmentDate;
}