package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO связи услуги и материала.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMaterialDto {
    private Long id;
    private Long serviceId;
    private Long materialId;
    private String materialName;
    private BigDecimal quantityUsed;
    private String notes;
}