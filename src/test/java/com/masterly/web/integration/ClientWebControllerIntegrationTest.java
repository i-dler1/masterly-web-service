package com.masterly.web.integration;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoreServiceClient coreServiceClient;

    private ClientDto testClient;
    private MasterDto testMaster;
    private ServiceEntityDto testService;
    private AppointmentDto testAppointment;

    @BeforeEach
    void setUp() {
        testClient = new ClientDto();
        testClient.setId(1L);
        testClient.setEmail("client@test.com");
        testClient.setFullName("Тестовый Клиент");
        testClient.setPhone("+375291234567");

        testMaster = new MasterDto();
        testMaster.setId(1L);
        testMaster.setFullName("Тестовый Мастер");
        testMaster.setSpecialization("Парикмахер");

        testService = new ServiceEntityDto();
        testService.setId(1L);
        testService.setName("Стрижка");
        testService.setDurationMinutes(60);
        testService.setIsActive(true);

        testAppointment = new AppointmentDto();
        testAppointment.setId(1L);
        testAppointment.setAppointmentDate(LocalDate.now().plusDays(1));
        testAppointment.setStartTime(LocalTime.of(10, 0));

        // НАСТРОЙКА МОКОВ — ИСПОЛЬЗУЕМ ArrayList ДЛЯ ИЗБЕЖАНИЯ UnsupportedOperationException
        when(coreServiceClient.getClientByEmail("client@test.com")).thenReturn(testClient);
        when(coreServiceClient.getMasterById(1L)).thenReturn(testMaster);
        when(coreServiceClient.getServicesByMasterId(1L)).thenReturn(new ArrayList<>(List.of(testService)));
        when(coreServiceClient.getAppointmentsByClientId(1L))
                .thenReturn(new ArrayList<>(List.of(testAppointment)));  // ← ИСПРАВЛЕНО!
        when(coreServiceClient.getAllMasters()).thenReturn(new ArrayList<>(List.of(testMaster)));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void clientDashboard_ShouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/dashboard"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void clientProfile_ShouldReturnProfileView() throws Exception {
        mockMvc.perform(get("/client/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/profile"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void myAppointments_ShouldReturnAppointmentsView() throws Exception {
        mockMvc.perform(get("/client/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/appointments"))
                .andExpect(model().attributeExists("appointments"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void masters_ShouldReturnMastersView() throws Exception {
        mockMvc.perform(get("/client/masters"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/masters"))
                .andExpect(model().attributeExists("masters"));
    }

    @Test
    @WithMockUser(username = "client@test.com", roles = "CLIENT")
    void viewMaster_ShouldReturnMasterDetailsView() throws Exception {
        mockMvc.perform(get("/client/masters/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/master-details"))
                .andExpect(model().attributeExists("master"))
                .andExpect(model().attributeExists("services"));
    }
}