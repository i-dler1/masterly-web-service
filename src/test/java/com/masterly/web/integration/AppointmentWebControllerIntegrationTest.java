package com.masterly.web.integration;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
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
class AppointmentWebControllerIntegrationTest {

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
        testClient.setEmail("client@test.com");

        testService = new ServiceEntityDto();
        testService.setId(1L);
        testService.setName("Стрижка");
        testService.setDurationMinutes(60);

        testAppointment = new AppointmentDto();
        testAppointment.setId(1L);
        testAppointment.setMasterId(1L);
        testAppointment.setClientId(1L);
        testAppointment.setClientName("Тестовый Клиент");
        testAppointment.setServiceId(1L);
        testAppointment.setServiceName("Стрижка");
        testAppointment.setAppointmentDate(LocalDate.now().plusDays(1));
        testAppointment.setStartTime(LocalTime.of(10, 0));
        testAppointment.setStatus("PENDING");

        when(coreServiceClient.getMasterByEmail("master@test.com")).thenReturn(testMaster);
        when(coreServiceClient.getMasterById(1L)).thenReturn(testMaster);
        when(coreServiceClient.getAllClients(1L)).thenReturn(List.of(testClient));
        when(coreServiceClient.getAllServices(1L)).thenReturn(List.of(testService));
        when(coreServiceClient.getAppointment(1L)).thenReturn(testAppointment);
    }

    @Test
    @WithMockUser(username = "master@test.com", roles = "MASTER")
    void listAppointments_ShouldReturnAppointmentsView() throws Exception {
        var page = new PageImpl<>(List.of(testAppointment));
        when(coreServiceClient.getAppointmentsPaginated(anyInt(), anyInt(), anyString(), anyString(), eq(1L)))
                .thenReturn(page);

        mockMvc.perform(get("/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointments/list"))
                .andExpect(model().attributeExists("appointments"));
    }

    @Test
    @WithMockUser(username = "master@test.com", roles = "MASTER")
    void showEditForm_ShouldReturnFormView() throws Exception {
        mockMvc.perform(get("/appointments/edit/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("appointments/form"))
                .andExpect(model().attributeExists("appointment"))
                .andExpect(model().attributeExists("clients"))
                .andExpect(model().attributeExists("services"));
    }

    @Test
    @WithMockUser(username = "master@test.com", roles = "MASTER")
    void deleteAppointment_ShouldRedirect() throws Exception {
        mockMvc.perform(get("/appointments/delete/{id}", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"));
    }

    @Test
    @WithMockUser(username = "master@test.com", roles = "MASTER")
    void updateStatus_ShouldRedirect() throws Exception {
        when(coreServiceClient.updateAppointmentStatus(eq(1L), eq("CONFIRMED")))
                .thenReturn(testAppointment);

        mockMvc.perform(get("/appointments/status/{id}", 1L)
                        .param("status", "CONFIRMED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"));
    }
}