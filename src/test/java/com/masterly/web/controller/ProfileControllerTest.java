package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.MasterUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private ProfileController profileController;

    private MasterDto masterDto;
    private String email;

    @BeforeEach
    void setUp() {
        email = "master@test.com";

        masterDto = new MasterDto();
        masterDto.setId(1L);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");
        masterDto.setPhone("+375291234567");
        masterDto.setBusinessName("Салон Красоты");
        masterDto.setSpecialization("Парикмахер");
        masterDto.setRole("MASTER");
    }

    // ==================== showProfile ====================

    @Test
    void showProfile_ShouldReturnAdminProfile_WhenUserIsAdmin() {
        // given
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        // when
        String viewName = profileController.showProfile(authentication, model);

        // then
        assertEquals("admin/profile", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model).addAttribute("master", masterDto);
    }

    @Test
    void showProfile_ShouldReturnMasterProfile_WhenUserIsMaster() {
        // given
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        // when
        String viewName = profileController.showProfile(authentication, model);

        // then
        assertEquals("masters/profile", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model).addAttribute("master", masterDto);
    }

    @Test
    void showProfile_ShouldReturnError_WhenExceptionThrown() {
        // given
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = profileController.showProfile(authentication, model);

        // then
        assertEquals("error", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model, never()).addAttribute(eq("master"), any());
    }

    // ==================== showEditForm ====================

    @Test
    void showEditForm_ShouldReturnProfileEditView() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        // when
        String viewName = profileController.showEditForm(authentication, model);

        // then
        assertEquals("profile/edit", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model).addAttribute(eq("master"), any(MasterUpdateDto.class));
    }

    @Test
    void showEditForm_ShouldReturnError_WhenExceptionThrown() {
        // given
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = profileController.showEditForm(authentication, model);

        // then
        assertEquals("error", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(model, never()).addAttribute(eq("master"), any());
    }

    // ==================== updateProfile ====================

    @Test
    void updateProfile_ShouldRedirectToProfile_WhenSuccess() {
        // given
        MasterUpdateDto updateDto = new MasterUpdateDto();
        updateDto.setFullName("Новое Имя");
        updateDto.setPhone("+375331112233");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.updateMasterProfile(eq(masterDto.getId()), any(MasterUpdateDto.class)))
                .thenReturn(masterDto);

        // when
        String viewName = profileController.updateProfile(updateDto, authentication);

        // then
        assertEquals("redirect:/profile", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient).updateMasterProfile(eq(masterDto.getId()), eq(updateDto));
    }

    @Test
    void updateProfile_ShouldRedirectWithError_WhenExceptionThrown() {
        // given
        MasterUpdateDto updateDto = new MasterUpdateDto();

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));

        // when
        String viewName = profileController.updateProfile(updateDto, authentication);

        // then
        assertEquals("redirect:/profile?error", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient, never()).updateMasterProfile(anyLong(), any());
    }

    @Test
    void updateProfile_ShouldRedirectWithError_WhenUpdateThrowsException() {
        // given
        MasterUpdateDto updateDto = new MasterUpdateDto();

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.updateMasterProfile(eq(masterDto.getId()), any()))
                .thenThrow(new RuntimeException("Update failed"));

        // when
        String viewName = profileController.updateProfile(updateDto, authentication);

        // then
        assertEquals("redirect:/profile?error", viewName);
        verify(authentication).getName();
        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient).updateMasterProfile(eq(masterDto.getId()), any());
    }
}