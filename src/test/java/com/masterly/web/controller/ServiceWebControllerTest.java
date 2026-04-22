package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.ServiceEntityDto;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceWebControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private ServiceWebController serviceWebController;

    private ServiceEntityDto serviceEntityDto;
    private MasterDto masterDto;
    private Page<ServiceEntityDto> servicePage;
    private String email;

    @BeforeEach
    void setUp() {
        email = "test@masterly.com";

        masterDto = new MasterDto();
        masterDto.setId(1L);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");

        serviceEntityDto = new ServiceEntityDto();
        serviceEntityDto.setId(1L);
        serviceEntityDto.setMasterId(1L);
        serviceEntityDto.setName("Тестовая услуга");
        serviceEntityDto.setDurationMinutes(60);
        serviceEntityDto.setPrice(BigDecimal.valueOf(1000));
        serviceEntityDto.setIsActive(true);

        List<ServiceEntityDto> services = Collections.singletonList(serviceEntityDto);
        servicePage = new PageImpl<>(services);
    }

    @Test
    void listServices_ShouldReturnServicesView_ForMaster() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(model).addAttribute(eq("services"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", servicePage.getTotalPages());
        verify(model).addAttribute("totalItems", servicePage.getTotalElements());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "id");
        verify(model).addAttribute("sortDir", "asc");
        verify(model).addAttribute("reverseSortDir", "desc");
    }

    @Test
    void listServices_ShouldReturnServicesView_ForAdmin() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getAllServicesForAdmin(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
        verify(model).addAttribute(eq("services"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", servicePage.getTotalPages());
        verify(model).addAttribute("totalItems", servicePage.getTotalElements());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "id");
        verify(model).addAttribute("sortDir", "asc");
        verify(model).addAttribute("reverseSortDir", "desc");
    }

    @Test
    void listServices_ShouldUseDefaultMasterId_WhenAuthenticationNull() {
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(coreServiceClient, never()).getMasterByEmail(anyString());
        verify(coreServiceClient).getServicesPaginated(0, 10, "id", "asc", 1L);
    }

    @Test
    void listServices_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(coreServiceClient).getMasterByEmail(email);
        verify(coreServiceClient).getServicesPaginated(0, 10, "id", "asc", 1L);
    }

    @Test
    void listServices_ShouldSetReverseSortDir_AscToDesc() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        verify(model).addAttribute("reverseSortDir", "desc");
    }

    @Test
    void listServices_ShouldSetReverseSortDir_DescToAsc() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        serviceWebController.listServices(0, 10, "id", "desc", authentication, model);

        verify(model).addAttribute("reverseSortDir", "asc");
    }

    @Test
    void listServices_ShouldUseDefaultPaginationValues() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(coreServiceClient).getServicesPaginated(0, 10, "id", "asc", 1L);
    }

    @Test
    void listServices_ShouldHandleCustomPaginationValues() {
        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);

        String viewName = serviceWebController.listServices(2, 20, "name", "desc", authentication, model);

        assertEquals("services/list", viewName);
        verify(coreServiceClient).getServicesPaginated(2, 20, "name", "desc", 1L);
        verify(model).addAttribute("currentPage", 2);
        verify(model).addAttribute("size", 20);
        verify(model).addAttribute("sortBy", "name");
        verify(model).addAttribute("sortDir", "desc");
        verify(model).addAttribute("reverseSortDir", "asc");
    }

    @Test
    void listServices_ShouldHandleEmptyPage() {
        Page<ServiceEntityDto> emptyPage = new PageImpl<>(Collections.emptyList());

        when(authentication.getName()).thenReturn(email);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_MASTER")))
                .when(authentication).getAuthorities();
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(emptyPage);

        String viewName = serviceWebController.listServices(0, 10, "id", "asc", authentication, model);

        assertEquals("services/list", viewName);
        verify(model).addAttribute(eq("services"), eq(Collections.emptyList()));
        verify(model).addAttribute("totalItems", 0L);
    }
}