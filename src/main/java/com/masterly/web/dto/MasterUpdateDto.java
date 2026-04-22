package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO обновления профиля мастера.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterUpdateDto {
    private String fullName;
    private String phone;
    private String businessName;
    private String specialization;
}