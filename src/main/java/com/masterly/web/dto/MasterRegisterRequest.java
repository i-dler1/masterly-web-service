package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на регистрацию мастера.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterRegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String role;
}