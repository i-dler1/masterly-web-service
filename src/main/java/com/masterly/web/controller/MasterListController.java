package com.masterly.web.controller;

import com.masterly.web.client.CoreServiceClient;
import com.masterly.web.dto.MasterDto;
import com.masterly.web.dto.ServiceEntityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Контроллер публичного просмотра мастеров.
 */
@Slf4j
@Controller
@RequestMapping("/masters")
@RequiredArgsConstructor
public class MasterListController {

    private final CoreServiceClient coreServiceClient;

    /**
     * Список всех мастеров.
     *
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping
    public String listMasters(Model model) {
        log.info("Listing all masters");
        List<MasterDto> masters = coreServiceClient.getAllMasters();
        model.addAttribute("masters", masters);
        return "masters/list";
    }

    /**
     * Профиль мастера с услугами.
     *
     * @param id идентификатор мастера
     * @param model модель для передачи данных в шаблон
     * @return название шаблона
     */
    @GetMapping("/{id}")
    public String masterProfile(@PathVariable Long id, Model model) {
        log.info("Viewing master profile: {}", id);
        MasterDto master = coreServiceClient.getMasterById(id);
        model.addAttribute("master", master);

        List<ServiceEntityDto> services = coreServiceClient.getServicesByMasterId(id);
        model.addAttribute("services", services);

        return "masters/profile";
    }
}