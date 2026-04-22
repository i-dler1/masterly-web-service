package com.masterly.web.config;

import com.masterly.web.service.TokenStorageService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Перехватчик Feign-запросов.
 * Подставляет JWT токен в заголовок Authorization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeignClientInterceptor implements RequestInterceptor {

    private final TokenStorageService tokenStorageService;

    /**
     * Добавляет токен в заголовок запроса.
     *
     * @param template шаблон запроса
     */
    @Override
    public void apply(RequestTemplate template) {
        String token = tokenStorageService.getToken();

        if (template.url().contains("/api/auth/login") ||
                template.url().contains("/api/masters")) {
            log.debug("Skipping token for auth request: {}", template.url());
            return;
        }

        log.debug("Feign interceptor - token present: {}", token != null);
        if (token != null && !token.isEmpty()) {
            log.debug("Token length: {}, adding to request: {}", token.length(), template.url());
            template.header("Authorization", "Bearer " + token);
        } else {
            log.debug("No token found for request to: {}", template.url());
        }
    }
}