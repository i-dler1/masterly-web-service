package com.masterly.web.dto;

import lombok.Data;

/**
 * Запрос на вход.
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}