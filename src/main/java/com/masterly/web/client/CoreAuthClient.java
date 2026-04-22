package com.masterly.web.client;

import com.masterly.web.dto.LoginRequest;
import com.masterly.web.dto.AuthResponse;
import com.masterly.web.dto.MasterRegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign клиент для аутентификации и регистрации.
 * Взаимодействует с auth-эндпоинтами core-service.
 */
@FeignClient(name = "core-auth", url = "${core.service.url}")
public interface CoreAuthClient {

    /**
     * Аутентификация мастера.
     *
     * @param request email и пароль
     * @return токен и данные мастера
     */
    @PostMapping("/api/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    /**
     * Регистрация нового мастера.
     *
     * @param request данные мастера
     * @return токен и данные созданного мастера
     */
    @PostMapping("/api/masters")
    AuthResponse register(@RequestBody MasterRegisterRequest request);
}