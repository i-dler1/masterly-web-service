package com.masterly.web.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Сервис для хранения JWT токена в сессии.
 */
@Service
@RequiredArgsConstructor
public class TokenStorageService {

    private static final String TOKEN_KEY = "jwt_token";
    private final HttpSession httpSession;

    /**
     * Сохраняет токен в сессию.
     *
     * @param token JWT токен
     */
    public void saveToken(String token) {
        httpSession.setAttribute(TOKEN_KEY, token);
    }

    /**
     * Возвращает токен из сессии.
     *
     * @return JWT токен или null
     */
    public String getToken() {
        return (String) httpSession.getAttribute(TOKEN_KEY);
    }

    /**
     * Удаляет токен из сессии.
     */
    public void clearToken() {
        httpSession.removeAttribute(TOKEN_KEY);
    }
}