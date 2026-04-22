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
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterListControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Model model;

    @InjectMocks
    private MasterListController masterListController;

    private MasterDto masterDto;
    private ServiceEntityDto serviceDto;

    @BeforeEach
    void setUp() {
        masterDto = new MasterDto();
        masterDto.setId(1L);
        masterDto.setFullName("Тестовый Мастер");
        masterDto.setEmail("master@test.com");
        masterDto.setSpecialization("Парикмахер");

        serviceDto = new ServiceEntityDto();
        serviceDto.setId(1L);
        serviceDto.setName("Стрижка");
    }

    @Test
    void listMasters_ShouldReturnMastersListView() {
        // given
        List<MasterDto> masters = Collections.singletonList(masterDto);
        when(coreServiceClient.getAllMasters()).thenReturn(masters);

        // when
        String viewName = masterListController.listMasters(model);

        // then
        assertEquals("masters/list", viewName);
        verify(coreServiceClient).getAllMasters();
        verify(model).addAttribute("masters", masters);
    }

    @Test
    void listMasters_ShouldReturnViewWithEmptyList_WhenNoMasters() {
        // given
        when(coreServiceClient.getAllMasters()).thenReturn(Collections.emptyList());

        // when
        String viewName = masterListController.listMasters(model);

        // then
        assertEquals("masters/list", viewName);
        verify(coreServiceClient).getAllMasters();
        verify(model).addAttribute(eq("masters"), anyList());
    }

    @Test
    void masterProfile_ShouldReturnMasterProfileView() {
        // given
        Long masterId = 1L;
        List<ServiceEntityDto> services = Collections.singletonList(serviceDto);

        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(services);

        // when
        String viewName = masterListController.masterProfile(masterId, model);

        // then
        assertEquals("masters/profile", viewName);
        verify(coreServiceClient).getMasterById(masterId);
        verify(coreServiceClient).getServicesByMasterId(masterId);
        verify(model).addAttribute("master", masterDto);
        verify(model).addAttribute("services", services);
    }

    @Test
    void masterProfile_ShouldReturnViewWithEmptyServices_WhenNoServices() {
        // given
        Long masterId = 1L;
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);
        when(coreServiceClient.getServicesByMasterId(masterId)).thenReturn(Collections.emptyList());

        // when
        String viewName = masterListController.masterProfile(masterId, model);

        // then
        assertEquals("masters/profile", viewName);
        verify(coreServiceClient).getMasterById(masterId);
        verify(coreServiceClient).getServicesByMasterId(masterId);
        verify(model).addAttribute("master", masterDto);
        verify(model).addAttribute(eq("services"), anyList());
    }
}