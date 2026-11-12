package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.TaxMethod;
import com.fashion.platform.finance.domain.entity.TaxType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxConfigCreateRequest {

    @NotNull(message = "Tax type is required")
    private TaxType taxType;

    private TaxMethod taxMethod;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String configJson;
}
