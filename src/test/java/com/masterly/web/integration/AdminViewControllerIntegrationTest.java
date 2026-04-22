//package com.masterly.web.integration;
//
//import com.masterly.web.client.CoreServiceClient;
//import com.masterly.web.dto.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//class AdminViewControllerIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private CoreServiceClient coreServiceClient;
//
//    private MasterDto adminDto;
//    private ServiceEntityDto serviceDto;
//    private MaterialDto materialDto;
//    private ClientDto clientDto;
//    private AppointmentDto appointmentDto;
//
//    @BeforeEach
//    void setUp() {
//        adminDto = new MasterDto();
//        adminDto.setId(1L);
//        adminDto.setEmail("admin@masterly.com");
//        adminDto.setFullName("Администратор");
//        adminDto.setRole("ADMIN");
//        adminDto.setIsActive(true);
//
//        serviceDto = new ServiceEntityDto();
//        serviceDto.setId(1L);
//        serviceDto.setName("Тестовая услуга");
//        serviceDto.setPrice(BigDecimal.valueOf(1000));
//
//        materialDto = new MaterialDto();
//        materialDto.setId(1L);
//        materialDto.setName("Тестовый материал");
//
//        clientDto = new ClientDto();
//        clientDto.setId(1L);
//        clientDto.setFullName("Тестовый клиент");
//
//        appointmentDto = new AppointmentDto();
//        appointmentDto.setId(1L);
//        appointmentDto.setAppointmentDate(LocalDate.now());
//        appointmentDto.setStartTime(LocalTime.of(10, 0));
//        appointmentDto.setEndTime(LocalTime.of(11, 0));
//
//        when(coreServiceClient.getMasterByEmail("admin@masterly.com")).thenReturn(adminDto);
//        when(coreServiceClient.getAllMasters()).thenReturn(List.of(adminDto));
//        when(coreServiceClient.getAllServicesForAdmin(anyInt(), anyInt(), anyString(), anyString()))
//                .thenReturn(new PageImpl<>(List.of(serviceDto)));
//        when(coreServiceClient.getAllMaterialsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
//                .thenReturn(new PageImpl<>(List.of(materialDto)));
//        when(coreServiceClient.getAllClientsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
//                .thenReturn(new PageImpl<>(List.of(clientDto)));
//        when(coreServiceClient.getAllAppointmentsForAdmin(anyInt(), anyInt(), anyString(), anyString()))
//                .thenReturn(new PageImpl<>(List.of(appointmentDto)));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void dashboard_ShouldReturnDashboardView() throws Exception {
//        mockMvc.perform(get("/admin/dashboard"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/dashboard"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void services_ShouldReturnServicesView() throws Exception {
//        mockMvc.perform(get("/admin/services"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/services"))
//                .andExpect(model().attributeExists("services"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void materials_ShouldReturnMaterialsView() throws Exception {
//        mockMvc.perform(get("/admin/materials"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/materials"))
//                .andExpect(model().attributeExists("materials"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void clients_ShouldReturnClientsView() throws Exception {
//        mockMvc.perform(get("/admin/clients"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/clients"))
//                .andExpect(model().attributeExists("clients"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void appointments_ShouldReturnAppointmentsView() throws Exception {
//        mockMvc.perform(get("/admin/appointments"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/appointments"))
//                .andExpect(model().attributeExists("appointments"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void masters_ShouldReturnMastersView() throws Exception {
//        mockMvc.perform(get("/admin/masters"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/masters"))
//                .andExpect(model().attributeExists("masters"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void profile_ShouldReturnProfileView() throws Exception {
//        mockMvc.perform(get("/admin/profile"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("admin/profile"))
//                .andExpect(model().attributeExists("admin"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void confirmAppointment_ShouldRedirect() throws Exception {
//        when(coreServiceClient.updateAppointmentStatus(eq(1L), eq("CONFIRMED")))
//                .thenReturn(appointmentDto);
//
//        mockMvc.perform(post("/admin/appointments/{id}/confirm", 1L))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/admin/appointments"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void completeAppointment_ShouldRedirect() throws Exception {
//        when(coreServiceClient.updateAppointmentStatus(eq(1L), eq("COMPLETED")))
//                .thenReturn(appointmentDto);
//
//        mockMvc.perform(post("/admin/appointments/{id}/complete", 1L))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/admin/appointments"));
//    }
//
//    @Test
//    @WithMockUser(username = "admin@masterly.com", roles = "ADMIN")
//    void cancelAppointment_ShouldRedirect() throws Exception {
//        when(coreServiceClient.updateAppointmentStatus(eq(1L), eq("CANCELLED")))
//                .thenReturn(appointmentDto);
//
//        mockMvc.perform(post("/admin/appointments/{id}/cancel", 1L))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/admin/appointments"));
//    }
//}