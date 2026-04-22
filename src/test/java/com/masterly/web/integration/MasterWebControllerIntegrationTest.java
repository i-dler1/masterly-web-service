package com.masterly.web.integration;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoreServiceClient coreServiceClient;

    private MasterDto testMaster;
    private ClientDto testClient;
    private ServiceEntityDto testService;
    private AppointmentDto testAppointment;

    @BeforeEach
    void setUp() {
        testMaster = new MasterDto();
        testMaster.setId(1L);
        testMaster.setEmail("master@test.com");
        testMaster.setFullName("Тестовый Мастер");
        testMaster.setRole("MASTER");

        testClient = new ClientDto();
        testClient.setId(1L);
        testClient.setFullName("Тестовый Клиент");
        testClient.setPhone("+375291234567");

        testService = new ServiceEntityDto();
        testService.setId(1L);
        testService.setName("Стрижка");
        testService.setDurationMinutes(60);
        testService.setPrice(java.math.BigDecimal.valueOf(1000));
        testService.setIsActive(true);

        testAppointment = new AppointmentDto();
        testAppointment.setId(1L);
        testAppointment.setMasterId(1L);
        testAppointment.setClientId(1L);
        testAppointment.setServiceId(1L);
        testAppointment.setAppointmentDate(LocalDate.now().plusDays(1));
        testAppointment.setStartTime(LocalTime.of(10, 0));
        testAppointment.setStatus("PENDING");

        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);

        Page<ServiceEntityDto> servicePage = new PageImpl<>(List.of(testService));
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(servicePage);
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void dashboard_ShouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/master/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/dashboard"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void services_ShouldReturnServicesView() throws Exception {
        // Given
        var page = new org.springframework.data.domain.PageImpl<>(List.of(testService));
        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getServicesPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/master/services"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/services"))
                .andExpect(model().attributeExists("services"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void clients_ShouldReturnClientsView() throws Exception {
        // Given
        var page = new org.springframework.data.domain.PageImpl<>(List.of(testClient));
        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getClientsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/master/clients"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/clients"))
                .andExpect(model().attributeExists("clients"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void appointments_ShouldReturnAppointmentsView() throws Exception {
        // Given
        var page = new org.springframework.data.domain.PageImpl<>(List.of(testAppointment));
        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getAppointmentsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/master/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/appointments"))
                .andExpect(model().attributeExists("appointments"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void profile_ShouldReturnProfileView() throws Exception {
        // Given
        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getMasterById(1L)).thenReturn(testMaster);

        // When & Then
        mockMvc.perform(get("/master/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/profile"))
                .andExpect(model().attributeExists("master"));
    }

    @Test
    @WithMockUser(roles = "MASTER")
    void materials_ShouldReturnMaterialsView() throws Exception {
        // Given
        var material = new MaterialDto();
        material.setId(1L);
        material.setName("Краска");
        var page = new org.springframework.data.domain.PageImpl<>(List.of(material));

        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getMaterialsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/master/materials"))
                .andExpect(status().isOk())
                .andExpect(view().name("masters/materials"))
                .andExpect(model().attributeExists("materials"));
    }
}