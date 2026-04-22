package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.MaterialDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialWebControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MaterialWebController materialWebController;

    private MaterialDto materialDto;
    private MasterDto masterDto;
    private Page<MaterialDto> materialPage;
    private String email;
    private Long masterId;
    private Long materialId;

    @BeforeEach
    void setUp() {
        email = "test@masterly.com";
        masterId = 1L;
        materialId = 1L;

        masterDto = new MasterDto();
        masterDto.setId(masterId);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");

        materialDto = new MaterialDto();
        materialDto.setId(materialId);
        materialDto.setName("Тестовый материал");
        materialDto.setUnit("шт");
        materialDto.setQuantity(100.0);
        materialDto.setPricePerUnit(BigDecimal.valueOf(500.0));

        List<MaterialDto> materials = Collections.singletonList(materialDto);
        materialPage = new PageImpl<>(materials);
    }

    @Test
    void listMaterials_ShouldReturnMaterialsView_ForMaster() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMaterialsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(materialPage);

        String viewName = materialWebController.listMaterials(0, 10, "id", "asc", authentication, model);

        assertEquals("materials/list", viewName);
        verify(model).addAttribute(eq("materials"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", materialPage.getTotalPages());
    }

    @Test
    void listMaterials_ShouldReturnMaterialsView_ForAdmin() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getAllMaterialsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(materialPage);

        String viewName = materialWebController.listMaterials(0, 10, "id", "asc", authentication, model);

        assertEquals("materials/list", viewName);
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
    }

    @Test
    void listMaterials_ShouldSetReverseSortDir() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMaterialsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(masterId)))
                .thenReturn(materialPage);

        materialWebController.listMaterials(0, 10, "id", "desc", authentication, model);

        verify(model).addAttribute("reverseSortDir", "asc");
    }

    @Test
    void listMaterials_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getMaterialsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(materialPage);

        String viewName = materialWebController.listMaterials(0, 10, "id", "asc", authentication, model);

        assertEquals("materials/list", viewName);
        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient).getMaterialsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L));
    }

    @Test
    void showCreateForm_ShouldReturnFormView() {
        String viewName = materialWebController.showCreateForm(model);

        assertEquals("materials/form", viewName);
        verify(model).addAttribute(eq("material"), any(MaterialDto.class));
    }

    @Test
    void showEditForm_ShouldReturnFormView_WithMaterial() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMaterial(materialId, masterId)).thenReturn(materialDto);

        String viewName = materialWebController.showEditForm(materialId, authentication, model);

        assertEquals("materials/form", viewName);
        verify(model).addAttribute("material", materialDto);
    }

    @Test
    void saveMaterial_ShouldCreateNewMaterial_WhenIdNull() {
        materialDto.setId(null);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.createMaterial(masterId, materialDto)).thenReturn(materialDto);

        String viewName = materialWebController.saveMaterial(materialDto, authentication);

        assertEquals("redirect:/materials", viewName);
        verify(coreServiceClient).createMaterial(masterId, materialDto);
        verify(coreServiceClient, never()).updateMaterial(any(), any(), any());
    }

    @Test
    void saveMaterial_ShouldUpdateExistingMaterial_WhenIdNotNull() {
        materialDto.setId(materialId);
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.updateMaterial(materialId, masterId, materialDto)).thenReturn(materialDto);

        String viewName = materialWebController.saveMaterial(materialDto, authentication);

        assertEquals("redirect:/materials", viewName);
        verify(coreServiceClient).updateMaterial(materialId, masterId, materialDto);
        verify(coreServiceClient, never()).createMaterial(any(), any());
    }

    @Test
    void deleteMaterial_ShouldDeleteAndRedirect() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        doNothing().when(coreServiceClient).deleteMaterial(materialId);

        String viewName = materialWebController.deleteMaterial(materialId, authentication);

        assertEquals("redirect:/materials", viewName);
        verify(coreServiceClient).deleteMaterial(materialId);
    }

    @Test
    void deleteMaterial_ShouldHandleException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        doThrow(new RuntimeException("Error")).when(coreServiceClient).deleteMaterial(materialId);

        String viewName = materialWebController.deleteMaterial(materialId, authentication);

        assertEquals("redirect:/materials?error", viewName);
        verify(coreServiceClient).deleteMaterial(materialId);
    }
}