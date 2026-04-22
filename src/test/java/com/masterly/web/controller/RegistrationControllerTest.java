package com.masterly.web.controller;

import com.masterly.web.client.CoreAuthClient;
import com.masterly.web.dto.AuthResponse;
import com.masterly.web.dto.MasterRegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private CoreAuthClient coreAuthClient;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    void showRegistrationForm_ShouldReturnRegisterView() {
        String viewName = registrationController.showRegistrationForm();
        assertEquals("register", viewName);
    }

    @Test
    void register_ShouldRedirectToLogin_WhenSuccess() {
        // Given
        String email = "new@masterly.com";
        String password = "123";
        String fullName = "Новый Мастер";
        String phone = "+79991234567";
        String role = "MASTER";

        AuthResponse response = new AuthResponse();
        when(coreAuthClient.register(any(MasterRegisterRequest.class))).thenReturn(response);

        // When
        String viewName = registrationController.register(email, password, fullName, phone, role, redirectAttributes);

        // Then
        assertEquals("redirect:/login", viewName);
        verify(coreAuthClient).register(any(MasterRegisterRequest.class));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void register_ShouldRedirectToRegisterWithError_WhenException() {
        // Given
        String email = "new@masterly.com";
        String password = "123";
        String fullName = "Новый Мастер";
        String phone = "+79991234567";
        String role = "MASTER";

        when(coreAuthClient.register(any(MasterRegisterRequest.class)))
                .thenThrow(new RuntimeException("Registration failed"));

        // When
        String viewName = registrationController.register(email, password, fullName, phone, role, redirectAttributes);

        // Then
        assertEquals("redirect:/register", viewName);
        verify(coreAuthClient).register(any(MasterRegisterRequest.class));
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void register_ShouldCreateCorrectMasterRegisterRequest() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String fullName = "Test Master";
        String phone = "+71234567890";
        String role = "MASTER";

        AuthResponse response = new AuthResponse();
        when(coreAuthClient.register(any(MasterRegisterRequest.class))).thenReturn(response);

        // When
        registrationController.register(email, password, fullName, phone, role, redirectAttributes);

        // Then
        verify(coreAuthClient).register(argThat(request ->
                request.getEmail().equals(email) &&
                        request.getPassword().equals(password) &&
                        request.getFullName().equals(fullName) &&
                        request.getPhone().equals(phone) &&
                        request.getRole().equals(role)
        ));
    }

    @Test
    void register_ShouldHandleDifferentRoles() {
        // Given
        String email = "client@example.com";
        String password = "client123";
        String fullName = "Test Client";
        String phone = "+71234567891";
        String role = "CLIENT";

        AuthResponse response = new AuthResponse();
        when(coreAuthClient.register(any(MasterRegisterRequest.class))).thenReturn(response);

        // When
        String viewName = registrationController.register(email, password, fullName, phone, role, redirectAttributes);

        // Then
        assertEquals("redirect:/login", viewName);
        verify(coreAuthClient).register(argThat(request -> request.getRole().equals("CLIENT")));
        verify(redirectAttributes).addFlashAttribute(eq("success"), anyString());
    }
}