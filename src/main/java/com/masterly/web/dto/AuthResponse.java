package com.masterly.web.dto;

import lombok.Data;

/**
 * Ответ аутентификации.
 */
@Data
public class AuthResponse {
    private String token;
    private String type;
    private Long id;
    private String email;
    private String name;
    private String role;
}