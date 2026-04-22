package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO услуги.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntityDto {
    private Long id;
    private Long masterId;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private String category;
    private Boolean isActive;
    private List<ServiceMaterialDto> materials;
}