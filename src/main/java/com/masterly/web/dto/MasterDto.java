package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO мастера.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDto {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String businessName;
    private String specialization;
    private String avatarUrl;
    private Boolean isActive;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}