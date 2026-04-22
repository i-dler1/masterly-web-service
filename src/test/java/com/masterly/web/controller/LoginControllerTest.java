package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private LoginController loginController;

    @Test
    void login_ShouldReturnLoginView() {
        String viewName = loginController.login();
        assertEquals("login", viewName);
    }

    @Test
    void home_ShouldRedirectToAdminDashboard_WhenAdmin() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();

        String viewName = loginController.home(authentication);

        assertEquals("redirect:/admin/dashboard", viewName);
    }

    @Test
    void home_ShouldRedirectToMasterDashboard_WhenMaster() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();

        String viewName = loginController.home(authentication);

        assertEquals("redirect:/master/dashboard", viewName);
    }

    @Test
    void home_ShouldRedirectToClientsDashboard_WhenClient() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")))
                .when(authentication).getAuthorities();

        String viewName = loginController.home(authentication);

        assertEquals("redirect:/clients/dashboard", viewName);
    }

    @Test
    void home_ShouldReturnIndex_WhenNoRole() {
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        String viewName = loginController.home(authentication);

        assertEquals("index", viewName);
    }

    @Test
    void accessDenied_ShouldReturnAccessDeniedView() {
        String viewName = loginController.accessDenied();
        assertEquals("access-denied", viewName);
    }
}