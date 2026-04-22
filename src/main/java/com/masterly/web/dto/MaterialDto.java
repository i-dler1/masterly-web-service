package com.masterly.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO материала.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal pricePerUnit;  // цена за единицу
    private Double quantity;          // количество
    private String unit;              // единица измерения
    private Double minQuantity;       // минимальный остаток
    private String supplier;          // поставщик
    private String category;
    private Boolean isActive;
    private String notes;
}