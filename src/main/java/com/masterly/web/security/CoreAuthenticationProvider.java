package com.masterly.web.security;

import com.masterly.web.client.CoreAuthClient;
import com.masterly.web.dto.AuthResponse;
import com.masterly.web.dto.LoginRequest;
import com.masterly.web.service.TokenStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Провайдер аутентификации через core-service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreAuthenticationProvider implements AuthenticationProvider {

    private final CoreAuthClient coreAuthClient;
    private final TokenStorageService tokenStorageService;

    /**
     * Аутентифицирует пользователя через core-service.
     *
     * @param authentication данные для входа
     * @return токен аутентификации
     * @throws AuthenticationException при ошибке
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("Authentication attempt for user: {}", email);

        try {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword(password);

            AuthResponse response = coreAuthClient.login(loginRequest);

            log.debug("Authentication successful for user: {}, role: {}", email, response.getRole());

            tokenStorageService.saveToken(response.getToken());
            log.debug("Token saved to session");

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + response.getRole()))
            );

            return authToken;

        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", email, e.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Проверяет, поддерживается ли тип аутентификации.
     *
     * @param authentication тип аутентификации
     * @return true если поддерживается
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}