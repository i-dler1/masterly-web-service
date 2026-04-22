package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.AppointmentDto;
import com.masterly.web.dto.ClientDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Контроллер просмотра записей клиента.
 */
@Slf4j
@Controller
@RequestMapping("/my-appointments")
@RequiredArgsConstructor
public class MyAppointmentsController {

    private final CoreServiceClient coreServiceClient;

    /**
     * Список записей текущего клиента.
     *
     * @param authentication данные аутентификации
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping
    public String myAppointments(Authentication authentication, Model model) {
        String email = authentication.getName();
        log.info("Viewing my appointments for user: {}", email);

        try {
            ClientDto client = coreServiceClient.getClientByEmail(email);
            List<AppointmentDto> appointments = coreServiceClient.getAppointmentsByClientId(client.getId());
            model.addAttribute("appointments", appointments);

            log.debug("Found {} appointments for client: {}", appointments.size(), email);

        } catch (Exception e) {
            log.error("Error loading appointments: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки записей");
        }

        return "my-appointments";
    }
}