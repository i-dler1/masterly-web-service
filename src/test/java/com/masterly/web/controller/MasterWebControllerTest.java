package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.*;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterWebControllerTest {

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MasterWebController masterWebController;

    private MasterDto masterDto;
    private String email;
    private Long masterId;

    @BeforeEach
    void setUp() {
        email = "test@masterly.com";
        masterId = 1L;

        masterDto = new MasterDto();
        masterDto.setId(masterId);
        masterDto.setEmail(email);
        masterDto.setFullName("Тестовый Мастер");
        masterDto.setPhone("+375291234567");
        masterDto.setBusinessName("Тестовый Салон");
        masterDto.setSpecialization("Парикмахер");
    }

    // ==================== БАЗОВЫЕ МЕТОДЫ ====================

    @Test
    void dashboard_ShouldReturnDashboardView() {
        String viewName = masterWebController.dashboard();
        assertEquals("masters/dashboard", viewName);
    }

    @Test
    void slots_ShouldReturnSlotsView() {
        String viewName = masterWebController.slots();
        assertEquals("masters/slots", viewName);
    }

    @Test
    void getMonthName_ShouldReturnCorrectRussianMonth() {
        // Test via calendar method
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllServices(masterId)).thenReturn(Collections.emptyList());

        String viewName = masterWebController.appointmentsCalendar("month", "2026-04-20", authentication, model);

        assertEquals("masters/appointments-calendar", viewName);
        verify(model).addAttribute("monthName", "Апрель");
    }

    // ==================== SERVICES ====================

    @Test
    void services_ShouldReturnServicesView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        Page<ServiceEntityDto> page = new PageImpl<>(Collections.emptyList());
        when(coreServiceClient.getServicesPaginated(0, 10, "name", "asc", masterId))
                .thenReturn(page);

        String viewName = masterWebController.services(model, authentication, 0, 10, "name", "asc");

        assertEquals("masters/services", viewName);
        verify(model).addAttribute(eq("services"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", page.getTotalPages());
        verify(model).addAttribute("totalItems", page.getTotalElements());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "name");
        verify(model).addAttribute("sortDir", "asc");
    }

    @Test
    void services_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getServicesPaginated(0, 10, "name", "asc", 1L))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        String viewName = masterWebController.services(model, authentication, 0, 10, "name", "asc");

        assertEquals("masters/services", viewName);
        verify(coreServiceClient).getServicesPaginated(0, 10, "name", "asc", 1L);
    }

    @Test
    void newServiceForm_ShouldReturnFormView_WithMaterials() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        List<MaterialDto> materials = List.of(new MaterialDto());
        when(coreServiceClient.getAllMaterials(masterId)).thenReturn(materials);

        String viewName = masterWebController.newServiceForm(model, authentication);

        assertEquals("masters/service-form", viewName);
        verify(model).addAttribute(eq("service"), any(ServiceEntityDto.class));
        verify(model).addAttribute("allMaterials", materials);
    }

    @Test
    void newServiceForm_ShouldHandleException_WhenLoadingMaterialsFails() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAllMaterials(masterId)).thenThrow(new RuntimeException("API error"));

        String viewName = masterWebController.newServiceForm(model, authentication);

        assertEquals("masters/service-form", viewName);
        verify(model).addAttribute(eq("service"), any(ServiceEntityDto.class));
        verify(model).addAttribute("allMaterials", List.of());
    }

    @Test
    void editServiceForm_ShouldReturnFormView_WithServiceAndMaterials() {
        Long serviceId = 1L;
        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");

        List<MaterialDto> materials = List.of(new MaterialDto());
        List<ServiceMaterialDto> serviceMaterials = List.of(new ServiceMaterialDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);
        when(coreServiceClient.getAllMaterials(masterId)).thenReturn(materials);
        when(coreServiceClient.getServiceMaterials(serviceId)).thenReturn(serviceMaterials);

        String viewName = masterWebController.editServiceForm(serviceId, authentication, model);

        assertEquals("masters/service-form", viewName);
        verify(model).addAttribute("service", service);
        verify(model).addAttribute("allMaterials", materials);
        verify(model).addAttribute("serviceMaterials", serviceMaterials);
    }

    @Test
    void saveService_ShouldCreateNewService_WhenIdIsNull() {
        ServiceEntityDto serviceDto = new ServiceEntityDto();
        serviceDto.setId(null);
        serviceDto.setName("Новая услуга");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveService(serviceDto, authentication);

        assertEquals("redirect:/master/services", viewName);
        verify(coreServiceClient).createService(masterId, serviceDto);
        verify(coreServiceClient, never()).updateService(anyLong(), anyLong(), any());
    }

    @Test
    void saveService_ShouldUpdateExistingService_WhenIdIsNotNull() {
        Long serviceId = 1L;
        ServiceEntityDto serviceDto = new ServiceEntityDto();
        serviceDto.setId(serviceId);
        serviceDto.setName("Обновленная услуга");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveService(serviceDto, authentication);

        assertEquals("redirect:/master/services", viewName);
        verify(coreServiceClient).updateService(serviceId, masterId, serviceDto);
        verify(coreServiceClient, never()).createService(anyLong(), any());
    }

    @Test
    void deleteService_ShouldDeleteAndRedirect_WhenSuccess() {
        Long serviceId = 1L;
        // coreServiceClient.deleteService() возвращает void? Если да, то doNothing() ок.
        // Проверьте сигнатуру метода в CoreServiceClient
        doNothing().when(coreServiceClient).deleteService(serviceId);

        String viewName = masterWebController.deleteService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(coreServiceClient).deleteService(serviceId);
        verify(redirectAttributes).addFlashAttribute("success", "Услуга успешно удалена");
    }

    @Test
    void deleteService_ShouldDeactivate_WhenConflictException() {
        Long serviceId = 1L;
        FeignException conflictException = mock(FeignException.class);
        when(conflictException.status()).thenReturn(409);
        doThrow(conflictException).when(coreServiceClient).deleteService(serviceId);
        doNothing().when(coreServiceClient).deactivateService(serviceId);

        String viewName = masterWebController.deleteService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(coreServiceClient).deactivateService(serviceId);
        verify(redirectAttributes).addFlashAttribute(eq("warning"), anyString());
    }

    @Test
    void deleteService_ShouldHandleException_WhenDeactivateFails() {
        Long serviceId = 1L;
        FeignException conflictException = mock(FeignException.class);
        when(conflictException.status()).thenReturn(409);
        doThrow(conflictException).when(coreServiceClient).deleteService(serviceId);
        doThrow(new RuntimeException("Deactivation failed")).when(coreServiceClient).deactivateService(serviceId);

        String viewName = masterWebController.deleteService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteService_ShouldHandleOtherFeignException() {
        Long serviceId = 1L;
        FeignException otherException = mock(FeignException.class);
        when(otherException.status()).thenReturn(500);
        when(otherException.getMessage()).thenReturn("Internal error");
        doThrow(otherException).when(coreServiceClient).deleteService(serviceId);

        String viewName = masterWebController.deleteService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteService_ShouldHandleGenericException() {
        Long serviceId = 1L;
        doThrow(new RuntimeException("Generic error")).when(coreServiceClient).deleteService(serviceId);

        String viewName = masterWebController.deleteService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void activateService_ShouldActivateAndRedirect_WhenSuccess() {
        Long serviceId = 1L;
        doNothing().when(coreServiceClient).activateService(serviceId);

        String viewName = masterWebController.activateService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(coreServiceClient).activateService(serviceId);
        verify(redirectAttributes).addFlashAttribute("success", "Услуга восстановлена");
    }

    @Test
    void activateService_ShouldHandleException() {
        Long serviceId = 1L;
        doThrow(new RuntimeException("Error")).when(coreServiceClient).activateService(serviceId);

        String viewName = masterWebController.activateService(serviceId, redirectAttributes);

        assertEquals("redirect:/master/services", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== MATERIALS ====================

    @Test
    void materials_ShouldReturnMaterialsView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        Page<MaterialDto> page = new PageImpl<>(Collections.emptyList());
        when(coreServiceClient.getMaterialsPaginated(0, 10, "name", "asc", masterId))
                .thenReturn(page);

        String viewName = masterWebController.materials(model, authentication, 0, 10, "name", "asc");

        assertEquals("masters/materials", viewName);
        verify(model).addAttribute(eq("materials"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", page.getTotalPages());
        verify(model).addAttribute("totalItems", page.getTotalElements());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "name");
        verify(model).addAttribute("sortDir", "asc");
    }

    @Test
    void materials_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getMaterialsPaginated(0, 10, "name", "asc", 1L))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        String viewName = masterWebController.materials(model, authentication, 0, 10, "name", "asc");

        assertEquals("masters/materials", viewName);
        verify(coreServiceClient).getMaterialsPaginated(0, 10, "name", "asc", 1L);
    }

    @Test
    void newMaterialForm_ShouldReturnFormView() {
        String viewName = masterWebController.newMaterialForm(model);

        assertEquals("masters/material-form", viewName);
        verify(model).addAttribute(eq("material"), any(MaterialDto.class));
    }

    @Test
    void editMaterialForm_ShouldReturnFormView_WithMaterial() {
        Long materialId = 1L;
        MaterialDto material = new MaterialDto();
        material.setId(materialId);
        material.setName("Краска");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMaterial(materialId, masterId)).thenReturn(material);

        String viewName = masterWebController.editMaterialForm(materialId, authentication, model);

        assertEquals("masters/material-form", viewName);
        verify(model).addAttribute("material", material);
    }

    @Test
    void saveMaterial_ShouldCreateNewMaterial_WhenIdIsNull() {
        MaterialDto materialDto = new MaterialDto();
        materialDto.setId(null);
        materialDto.setName("Новый материал");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveMaterial(materialDto, authentication);

        assertEquals("redirect:/master/materials", viewName);
        verify(coreServiceClient).createMaterial(masterId, materialDto);
        verify(coreServiceClient, never()).updateMaterial(anyLong(), anyLong(), any());
    }

    @Test
    void saveMaterial_ShouldUpdateExistingMaterial_WhenIdIsNotNull() {
        Long materialId = 1L;
        MaterialDto materialDto = new MaterialDto();
        materialDto.setId(materialId);
        materialDto.setName("Обновленный материал");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveMaterial(materialDto, authentication);

        assertEquals("redirect:/master/materials", viewName);
        verify(coreServiceClient).updateMaterial(materialId, masterId, materialDto);
        verify(coreServiceClient, never()).createMaterial(anyLong(), any());
    }

    @Test
    void deleteMaterial_ShouldDeleteAndRedirect_WhenSuccess() {
        Long materialId = 1L;
        doNothing().when(coreServiceClient).deleteMaterial(materialId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteMaterial(materialId, authentication, redirectAttributes);

        assertEquals("redirect:/master/materials", viewName);
        verify(coreServiceClient).deleteMaterial(materialId);
        verify(redirectAttributes).addFlashAttribute("success", "Материал удален");
    }

    @Test
    void deleteMaterial_ShouldHandleConflictException() {
        Long materialId = 1L;
        FeignException conflictException = mock(FeignException.class);
        when(conflictException.status()).thenReturn(409);
        doThrow(conflictException).when(coreServiceClient).deleteMaterial(materialId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteMaterial(materialId, authentication, redirectAttributes);

        assertEquals("redirect:/master/materials", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("используется"));
    }

    @Test
    void deleteMaterial_ShouldHandleOtherFeignException() {
        Long materialId = 1L;
        FeignException otherException = mock(FeignException.class);
        when(otherException.status()).thenReturn(500);
        doThrow(otherException).when(coreServiceClient).deleteMaterial(materialId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteMaterial(materialId, authentication, redirectAttributes);

        assertEquals("redirect:/master/materials", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== CLIENTS ====================

    @Test
    void clients_ShouldReturnClientsView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        Page<ClientDto> page = new PageImpl<>(Collections.emptyList());
        when(coreServiceClient.getClientsPaginated(0, 10, "fullName", "asc", masterId))
                .thenReturn(page);

        String viewName = masterWebController.clients(model, authentication, 0, 10, "fullName", "asc");

        assertEquals("masters/clients", viewName);
        verify(model).addAttribute(eq("clients"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", page.getTotalPages());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "fullName");
        verify(model).addAttribute("sortDir", "asc");
    }

    @Test
    void clients_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getClientsPaginated(0, 10, "fullName", "asc", 1L))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        String viewName = masterWebController.clients(model, authentication, 0, 10, "fullName", "asc");

        assertEquals("masters/clients", viewName);
        verify(coreServiceClient).getClientsPaginated(0, 10, "fullName", "asc", 1L);
    }

    @Test
    void newClientForm_ShouldReturnFormView() {
        String viewName = masterWebController.newClientForm(model);

        assertEquals("masters/client-form", viewName);
        verify(model).addAttribute(eq("client"), any(ClientDto.class));
    }

    @Test
    void editClientForm_ShouldReturnFormView_WithClient() {
        Long clientId = 1L;
        ClientDto client = new ClientDto();
        client.setId(clientId);
        client.setFullName("Иван Иванов");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getClient(clientId, masterId)).thenReturn(client);

        String viewName = masterWebController.editClientForm(clientId, authentication, model);

        assertEquals("masters/client-form", viewName);
        verify(model).addAttribute("client", client);
    }

    @Test
    void saveClient_ShouldCreateNewClient_WhenIdIsNull() {
        ClientDto clientDto = new ClientDto();
        clientDto.setId(null);
        clientDto.setFullName("Новый клиент");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveClient(clientDto, authentication);

        assertEquals("redirect:/master/clients", viewName);
        verify(coreServiceClient).createClient(masterId, clientDto);
        verify(coreServiceClient, never()).updateClient(anyLong(), anyLong(), any());
    }

    @Test
    void saveClient_ShouldUpdateExistingClient_WhenIdIsNotNull() {
        Long clientId = 1L;
        ClientDto clientDto = new ClientDto();
        clientDto.setId(clientId);
        clientDto.setFullName("Обновленный клиент");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.saveClient(clientDto, authentication);

        assertEquals("redirect:/master/clients", viewName);
        verify(coreServiceClient).updateClient(clientId, masterId, clientDto);
        verify(coreServiceClient, never()).createClient(anyLong(), any());
    }

    @Test
    void deleteClient_ShouldDeleteAndRedirect_WhenSuccess() {
        Long clientId = 1L;
        doNothing().when(coreServiceClient).deleteClient(clientId, masterId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteClient(clientId, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);
        verify(coreServiceClient).deleteClient(clientId, masterId);
        verify(redirectAttributes).addFlashAttribute("success", "Клиент успешно удален");
    }

    @Test
    void deleteClient_ShouldHandleConflictException() {
        Long clientId = 1L;
        FeignException conflictException = mock(FeignException.class);
        when(conflictException.status()).thenReturn(409);
        doThrow(conflictException).when(coreServiceClient).deleteClient(clientId, masterId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteClient(clientId, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("есть записи"));
    }

    @Test
    void deleteClient_ShouldHandleOtherFeignException() {
        Long clientId = 1L;
        FeignException otherException = mock(FeignException.class);
        when(otherException.status()).thenReturn(500);
        doThrow(otherException).when(coreServiceClient).deleteClient(clientId, masterId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteClient(clientId, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void deleteClient_ShouldHandleGenericException() {
        Long clientId = 1L;
        doThrow(new RuntimeException("Generic error")).when(coreServiceClient).deleteClient(clientId, masterId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        String viewName = masterWebController.deleteClient(clientId, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== APPOINTMENTS ====================

    @Test
    void appointments_ShouldReturnAppointmentsView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        Page<AppointmentDto> page = new PageImpl<>(Collections.emptyList());
        when(coreServiceClient.getAppointmentsPaginated(0, 10, "appointmentDate", "desc", masterId))
                .thenReturn(page);

        String viewName = masterWebController.appointments(model, authentication, 0, 10, "appointmentDate", "desc");

        assertEquals("masters/appointments", viewName);
        verify(model).addAttribute(eq("appointments"), anyList());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", page.getTotalPages());
        verify(model).addAttribute("size", 10);
        verify(model).addAttribute("sortBy", "appointmentDate");
        verify(model).addAttribute("sortDir", "desc");
        verify(model).addAttribute(eq("today"), any(LocalDate.class));
    }

    @Test
    void appointments_ShouldUseDefaultMasterId_WhenGetMasterByEmailThrowsException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenThrow(new RuntimeException("API error"));
        when(coreServiceClient.getAppointmentsPaginated(0, 10, "appointmentDate", "desc", 1L))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        String viewName = masterWebController.appointments(model, authentication, 0, 10, "appointmentDate", "desc");

        assertEquals("masters/appointments", viewName);
        verify(coreServiceClient).getAppointmentsPaginated(0, 10, "appointmentDate", "desc", 1L);
    }

    @Test
    void confirmAppointment_ShouldConfirmAndRedirect_WhenSuccess() {
        Long appointmentId = 1L;
        AppointmentDto updatedAppointment = new AppointmentDto();
        updatedAppointment.setId(appointmentId);
        updatedAppointment.setStatus("CONFIRMED");

        when(coreServiceClient.updateAppointmentStatus(appointmentId, "CONFIRMED"))
                .thenReturn(updatedAppointment);  // ✅ Правильно!

        String viewName = masterWebController.confirmAppointment(appointmentId, redirectAttributes);

        assertEquals("redirect:/master/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(appointmentId, "CONFIRMED");
        verify(redirectAttributes).addFlashAttribute("success", "Запись подтверждена");
    }

    @Test
    void confirmAppointment_ShouldHandleException() {
        Long appointmentId = 1L;
        doThrow(new RuntimeException("Error")).when(coreServiceClient).updateAppointmentStatus(appointmentId, "CONFIRMED");

        String viewName = masterWebController.confirmAppointment(appointmentId, redirectAttributes);

        assertEquals("redirect:/master/appointments", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void cancelAppointment_ShouldCancelAndRedirect_WhenSuccess() {
        Long appointmentId = 1L;
        AppointmentDto updatedAppointment = new AppointmentDto();
        updatedAppointment.setId(appointmentId);
        updatedAppointment.setStatus("CANCELLED");

        when(coreServiceClient.updateAppointmentStatus(appointmentId, "CANCELLED"))
                .thenReturn(updatedAppointment);

        String viewName = masterWebController.cancelAppointment(appointmentId, redirectAttributes);

        assertEquals("redirect:/master/appointments", viewName);
        verify(coreServiceClient).updateAppointmentStatus(appointmentId, "CANCELLED");
        verify(redirectAttributes).addFlashAttribute("success", "Запись отменена");
    }

    @Test
    void cancelAppointment_ShouldHandleException() {
        Long appointmentId = 1L;
        doThrow(new RuntimeException("Error")).when(coreServiceClient).updateAppointmentStatus(appointmentId, "CANCELLED");

        String viewName = masterWebController.cancelAppointment(appointmentId, redirectAttributes);

        assertEquals("redirect:/master/appointments", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void appointmentsCalendar_ShouldReturnCalendarView_WeekView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllServices(masterId)).thenReturn(Collections.emptyList());

        String viewName = masterWebController.appointmentsCalendar("week", "2026-04-20", authentication, model);

        assertEquals("masters/appointments-calendar", viewName);
        verify(model).addAttribute("appointments", Collections.emptyList());
        verify(model).addAttribute("slots", Collections.emptyList());
        verify(model).addAttribute("services", Collections.emptyList());
        verify(model).addAttribute(eq("currentDate"), any(LocalDate.class));
        verify(model).addAttribute(eq("startDate"), any(LocalDate.class));
        verify(model).addAttribute("view", "week");
        verify(model).addAttribute(eq("monthName"), anyString());
        verify(model).addAttribute(eq("year"), anyInt());
        verify(model).addAttribute(eq("today"), any(LocalDate.class));
    }

    @Test
    void appointmentsCalendar_ShouldReturnCalendarView_MonthView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllServices(masterId)).thenReturn(Collections.emptyList());

        String viewName = masterWebController.appointmentsCalendar("month", null, authentication, model);

        assertEquals("masters/appointments-calendar", viewName);
        verify(model).addAttribute("view", "month");
        verify(model).addAttribute(eq("daysInRange"), anyList());
    }

    @Test
    void appointmentsCalendar_ShouldUseDefaultValues_WhenParamsNull() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllServices(masterId)).thenReturn(Collections.emptyList());

        String viewName = masterWebController.appointmentsCalendar(null, null, authentication, model);

        assertEquals("masters/appointments-calendar", viewName);
        verify(model).addAttribute("view", "week");
        verify(model).addAttribute(eq("currentDate"), any(LocalDate.class));
    }

    @Test
    void appointmentsCalendar_ShouldHandleServicesException() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAppointmentsByMasterId(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(Collections.emptyList());
        when(coreServiceClient.getAllServices(masterId)).thenThrow(new RuntimeException("API error"));

        String viewName = masterWebController.appointmentsCalendar("week", null, authentication, model);

        assertEquals("masters/appointments-calendar", viewName);
        verify(model).addAttribute("services", List.of());
    }

    // ==================== PROFILE ====================

    @Test
    void profile_ShouldReturnProfileView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);

        String viewName = masterWebController.profile(authentication, model);

        assertEquals("masters/profile", viewName);
        verify(model).addAttribute("master", masterDto);
    }

    @Test
    void showEditProfileForm_ShouldReturnProfileEditView() {
        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getMasterById(masterId)).thenReturn(masterDto);

        String viewName = masterWebController.showEditProfileForm(authentication, model);

        assertEquals("masters/profile-edit", viewName);
        verify(model).addAttribute("master", masterDto);
    }

    @Test
    void updateProfile_ShouldUpdateAndRedirect_WhenSuccess() {
        MasterDto updatedMaster = new MasterDto();
        updatedMaster.setFullName("Обновленное имя");
        updatedMaster.setPhone("+375291234567");
        updatedMaster.setBusinessName("Новый салон");
        updatedMaster.setSpecialization("Новая специализация");

        MasterDto returnedMaster = new MasterDto();
        returnedMaster.setId(masterId);
        returnedMaster.setFullName("Обновленное имя");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.updateMasterProfile(eq(masterId), any(MasterUpdateDto.class)))
                .thenReturn(returnedMaster);  // ✅ Добавлен thenReturn

        String viewName = masterWebController.updateProfile(updatedMaster, authentication, redirectAttributes);

        assertEquals("redirect:/master/profile", viewName);

        ArgumentCaptor<MasterUpdateDto> captor = ArgumentCaptor.forClass(MasterUpdateDto.class);
        verify(coreServiceClient).updateMasterProfile(eq(masterId), captor.capture());

        MasterUpdateDto capturedDto = captor.getValue();
        assertEquals("Обновленное имя", capturedDto.getFullName());
        assertEquals("+375291234567", capturedDto.getPhone());
        assertEquals("Новый салон", capturedDto.getBusinessName());
        assertEquals("Новая специализация", capturedDto.getSpecialization());

        verify(redirectAttributes).addFlashAttribute("success", "Профиль успешно обновлен");
    }

    @Test
    void updateProfile_ShouldHandleException() {
        MasterDto updatedMaster = new MasterDto();
        updatedMaster.setFullName("Обновленное имя");

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        doThrow(new RuntimeException("Update failed")).when(coreServiceClient)
                .updateMasterProfile(eq(masterId), any(MasterUpdateDto.class));

        String viewName = masterWebController.updateProfile(updatedMaster, authentication, redirectAttributes);

        assertEquals("redirect:/master/profile", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== API METHODS ====================

    @Test
    void getServicesData_ShouldReturnServicesList() {
        List<ServiceEntityDto> services = List.of(new ServiceEntityDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAllServices(masterId)).thenReturn(services);

        List<ServiceEntityDto> result = masterWebController.getServicesData(authentication);

        assertEquals(services, result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getSlots_ShouldReturnSlots_WithoutServiceId() {
        String date = "2026-04-20";
        List<AvailabilitySlotDto> slots = List.of(new AvailabilitySlotDto());

        when(coreServiceClient.getFreeSlots(masterId, null, date)).thenReturn(slots);

        List<AvailabilitySlotDto> result = masterWebController.getSlots(masterId, date, null, authentication);

        assertEquals(slots, result);
        verify(coreServiceClient).getFreeSlots(masterId, null, date);
    }

    @Test
    void getSlots_ShouldReturnSlots_WithServiceId() {
        String date = "2026-04-20";
        Long serviceId = 1L;
        List<AvailabilitySlotDto> slots = List.of(new AvailabilitySlotDto());

        when(coreServiceClient.getFreeSlots(masterId, serviceId, date)).thenReturn(slots);

        List<AvailabilitySlotDto> result = masterWebController.getSlots(masterId, date, serviceId, authentication);

        assertEquals(slots, result);
        verify(coreServiceClient).getFreeSlots(masterId, serviceId, date);
    }

    @Test
    void createSlot_ShouldCreateAndReturnSlot_WhenValid() {
        AvailabilitySlotDto slotDto = new AvailabilitySlotDto();
        slotDto.setSlotDate(LocalDate.now().plusDays(1));
        slotDto.setStartTime(LocalTime.of(10, 0));
        slotDto.setEndTime(LocalTime.of(11, 0));
        slotDto.setServiceId(1L);

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(1L);
        service.setIsActive(true);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getService(1L, masterId)).thenReturn(service);
        when(coreServiceClient.createSlot(slotDto)).thenReturn(slotDto);

        AvailabilitySlotDto result = masterWebController.createSlot(slotDto, authentication);

        assertEquals(slotDto, result);
        assertEquals(masterId, slotDto.getMasterId());
        verify(coreServiceClient).createSlot(slotDto);
    }

    @Test
    void createSlot_ShouldThrowException_WhenSlotDateInPast() {
        AvailabilitySlotDto slotDto = new AvailabilitySlotDto();
        slotDto.setSlotDate(LocalDate.now().minusDays(1));
        slotDto.setStartTime(LocalTime.of(10, 0));
        slotDto.setEndTime(LocalTime.of(11, 0));

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        assertThrows(RuntimeException.class, () -> {
            masterWebController.createSlot(slotDto, authentication);
        });
    }

    @Test
    void createSlot_ShouldThrowException_WhenStartTimeNotBeforeEndTime() {
        AvailabilitySlotDto slotDto = new AvailabilitySlotDto();
        slotDto.setSlotDate(LocalDate.now().plusDays(1));
        slotDto.setStartTime(LocalTime.of(11, 0));
        slotDto.setEndTime(LocalTime.of(10, 0));

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        assertThrows(RuntimeException.class, () -> {
            masterWebController.createSlot(slotDto, authentication);
        });
    }

    @Test
    void createSlot_ShouldThrowException_WhenServiceNotActive() {
        AvailabilitySlotDto slotDto = new AvailabilitySlotDto();
        slotDto.setSlotDate(LocalDate.now().plusDays(1));
        slotDto.setStartTime(LocalTime.of(10, 0));
        slotDto.setEndTime(LocalTime.of(11, 0));
        slotDto.setServiceId(1L);

        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(1L);
        service.setIsActive(false);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getService(1L, masterId)).thenReturn(service);

        assertThrows(RuntimeException.class, () -> {
            masterWebController.createSlot(slotDto, authentication);
        });
    }

    @Test
    void createSlot_ShouldThrowException_WhenServiceNotFound() {
        AvailabilitySlotDto slotDto = new AvailabilitySlotDto();
        slotDto.setSlotDate(LocalDate.now().plusDays(1));
        slotDto.setStartTime(LocalTime.of(10, 0));
        slotDto.setEndTime(LocalTime.of(11, 0));
        slotDto.setServiceId(1L);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getService(1L, masterId)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            masterWebController.createSlot(slotDto, authentication);
        });
    }

    @Test
    void deleteSlot_ShouldDeleteSlot() {
        Long slotId = 1L;
        doNothing().when(coreServiceClient).deleteSlot(slotId);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);

        assertDoesNotThrow(() -> {
            masterWebController.deleteSlot(slotId, authentication);
        });

        verify(coreServiceClient).deleteSlot(slotId);
    }

    @Test
    void getAllSlots_ShouldReturnAllSlots() {
        List<AvailabilitySlotDto> slots = List.of(new AvailabilitySlotDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAllSlots(masterId)).thenReturn(slots);

        List<AvailabilitySlotDto> result = masterWebController.getAllSlots(authentication);

        assertEquals(slots, result);
    }

    @Test
    void getSlotsByDate_ShouldReturnSlotsForDate() {
        String date = "2026-04-20";
        List<AvailabilitySlotDto> slots = List.of(new AvailabilitySlotDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getFreeSlots(masterId, null, date)).thenReturn(slots);

        List<AvailabilitySlotDto> result = masterWebController.getSlotsByDate(date, authentication);

        assertEquals(slots, result);
        verify(coreServiceClient).getFreeSlots(masterId, null, date);
    }

    @Test
    void getActiveServices_ShouldReturnOnlyActiveServices() {
        ServiceEntityDto active = new ServiceEntityDto();
        active.setId(1L);
        active.setIsActive(true);

        ServiceEntityDto inactive = new ServiceEntityDto();
        inactive.setId(2L);
        inactive.setIsActive(false);

        List<ServiceEntityDto> allServices = List.of(active, inactive);

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAllServices(masterId)).thenReturn(allServices);

        List<ServiceEntityDto> result = masterWebController.getActiveServices(authentication);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getAllMaterials_ShouldReturnAllMaterials() {
        List<MaterialDto> materials = List.of(new MaterialDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getAllMaterials(masterId)).thenReturn(materials);

        List<MaterialDto> result = masterWebController.getAllMaterials(authentication);

        assertEquals(materials, result);
    }

    // ==================== SERVICE MATERIALS MANAGEMENT ====================

    @Test
    void serviceMaterials_ShouldReturnServiceMaterialsView() {
        Long serviceId = 1L;
        ServiceEntityDto service = new ServiceEntityDto();
        service.setId(serviceId);
        service.setName("Стрижка");

        List<MaterialDto> allMaterials = List.of(new MaterialDto());
        List<ServiceMaterialDto> serviceMaterials = List.of(new ServiceMaterialDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getService(serviceId, masterId)).thenReturn(service);
        when(coreServiceClient.getAllMaterials(masterId)).thenReturn(allMaterials);
        when(coreServiceClient.getServiceMaterials(serviceId)).thenReturn(serviceMaterials);

        String viewName = masterWebController.serviceMaterials(serviceId, authentication, model);

        assertEquals("masters/service-materials", viewName);
        verify(model).addAttribute("serviceId", serviceId);
        verify(model).addAttribute("serviceName", service.getName());
        verify(model).addAttribute("allMaterials", allMaterials);
        verify(model).addAttribute("serviceMaterials", serviceMaterials);
    }

    @Test
    void addMaterialToService_ShouldAddAndRedirect_WhenSuccess() {
        Long serviceId = 1L;
        Long materialId = 2L;
        BigDecimal quantityUsed = new BigDecimal("1.5");

        doNothing().when(coreServiceClient).addMaterialToService(serviceId, materialId, quantityUsed, null);

        String viewName = masterWebController.addMaterialToService(serviceId, materialId, quantityUsed, redirectAttributes);

        assertEquals("redirect:/master/services/" + serviceId + "/materials", viewName);
        verify(coreServiceClient).addMaterialToService(serviceId, materialId, quantityUsed, null);
        verify(redirectAttributes).addFlashAttribute("success", "Материал добавлен");
    }

    @Test
    void addMaterialToService_ShouldHandleException() {
        Long serviceId = 1L;
        Long materialId = 2L;
        BigDecimal quantityUsed = new BigDecimal("1.5");

        doThrow(new RuntimeException("Error")).when(coreServiceClient)
                .addMaterialToService(serviceId, materialId, quantityUsed, null);

        String viewName = masterWebController.addMaterialToService(serviceId, materialId, quantityUsed, redirectAttributes);

        assertEquals("redirect:/master/services/" + serviceId + "/materials", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    void removeMaterialFromService_ShouldRemoveAndRedirect_WhenSuccess() {
        Long serviceId = 1L;
        Long materialLinkId = 2L;

        doNothing().when(coreServiceClient).removeMaterialFromService(materialLinkId);

        String viewName = masterWebController.removeMaterialFromService(serviceId, materialLinkId, redirectAttributes);

        assertEquals("redirect:/master/services/" + serviceId + "/materials", viewName);
        verify(coreServiceClient).removeMaterialFromService(materialLinkId);
        verify(redirectAttributes).addFlashAttribute("success", "Материал удален");
    }

    @Test
    void removeMaterialFromService_ShouldHandleException() {
        Long serviceId = 1L;
        Long materialLinkId = 2L;

        doThrow(new RuntimeException("Error")).when(coreServiceClient).removeMaterialFromService(materialLinkId);

        String viewName = masterWebController.removeMaterialFromService(serviceId, materialLinkId, redirectAttributes);

        assertEquals("redirect:/master/services/" + serviceId + "/materials", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    // ==================== BOOK FOR CLIENT ====================

    @Test
    void bookForClient_ShouldReturnBookingForm() {
        Long clientId = 1L;
        ClientDto client = new ClientDto();
        client.setId(clientId);
        client.setFullName("Иван Иванов");

        List<ServiceEntityDto> services = List.of(new ServiceEntityDto());

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.getClient(clientId, masterId)).thenReturn(client);
        when(coreServiceClient.getAllServices(masterId)).thenReturn(services);

        String viewName = masterWebController.bookForClient(clientId, model, authentication);

        assertEquals("masters/book-for-client", viewName);
        verify(model).addAttribute("client", client);
        verify(model).addAttribute("services", services);
    }

    @Test
    void saveBookingForClient_ShouldCreateAppointmentAndRedirect_WhenSuccess() {
        Long clientId = 1L;
        Long serviceId = 2L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";
        String notes = "Тестовая запись";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class)))
                .thenReturn(new AppointmentDto());

        String viewName = masterWebController.saveBookingForClient(
                clientId, serviceId, appointmentDate, startTime, notes, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);

        ArgumentCaptor<AppointmentCreateDto> captor = ArgumentCaptor.forClass(AppointmentCreateDto.class);
        verify(coreServiceClient).createAppointment(captor.capture());

        AppointmentCreateDto capturedDto = captor.getValue();
        assertEquals(masterId, capturedDto.getMasterId());
        assertEquals(clientId, capturedDto.getClientId());
        assertEquals(serviceId, capturedDto.getServiceId());
        assertEquals(LocalDate.parse(appointmentDate), capturedDto.getAppointmentDate());
        assertEquals(LocalTime.parse(startTime), capturedDto.getStartTime());
        assertEquals(notes, capturedDto.getNotes());

        verify(redirectAttributes).addFlashAttribute("success", "Запись успешно создана!");
    }

    @Test
    void saveBookingForClient_ShouldHandleException() {
        Long clientId = 1L;
        Long serviceId = 2L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class)))
                .thenThrow(new RuntimeException("API error"));

        String viewName = masterWebController.saveBookingForClient(
                clientId, serviceId, appointmentDate, startTime, null, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);
        verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Ошибка при создании записи"));
    }

    @Test
    void saveBookingForClient_ShouldHandleNullNotes() {
        Long clientId = 1L;
        Long serviceId = 2L;
        String appointmentDate = "2026-04-20";
        String startTime = "10:00";

        when(authentication.getName()).thenReturn(email);
        when(coreServiceClient.getMasterByEmail(email)).thenReturn(masterDto);
        when(coreServiceClient.createAppointment(any(AppointmentCreateDto.class)))
                .thenReturn(new AppointmentDto());

        String viewName = masterWebController.saveBookingForClient(
                clientId, serviceId, appointmentDate, startTime, null, authentication, redirectAttributes);

        assertEquals("redirect:/master/clients", viewName);

        ArgumentCaptor<AppointmentCreateDto> captor = ArgumentCaptor.forClass(AppointmentCreateDto.class);
        verify(coreServiceClient).createAppointment(captor.capture());

        assertNull(captor.getValue().getNotes());
    }
}