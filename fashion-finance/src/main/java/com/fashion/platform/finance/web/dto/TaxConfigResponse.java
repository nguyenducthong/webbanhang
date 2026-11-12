package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.TaxConfiguration;
import com.fashion.platform.finance.domain.entity.TaxMethod;
import com.fashion.platform.finance.domain.entity.TaxType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class TaxConfigResponse {
    UUID id;
    UUID legalEntityId;
    TaxType taxType;
    TaxMethod taxMethod;
    String configJson;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
    UUID createdBy;
    UUID approvedBy;
    Instant approvedAt;
    boolean approved;
    Instant createdAt;

    public static TaxConfigResponse from(TaxConfiguration tc) {
        return TaxConfigResponse.builder()
                .id(tc.getId())
                .legalEntityId(tc.getLegalEntity().getId())
                .taxType(tc.getTaxType())
                .taxMethod(tc.getTaxMethod())
                .configJson(tc.getConfigJson())
                .effectiveFrom(tc.getEffectiveFrom())
                .effectiveTo(tc.getEffectiveTo())
                .createdBy(tc.getCreatedBy())
                .approvedBy(tc.getApprovedBy())
                .approvedAt(tc.getApprovedAt())
                .approved(tc.isApproved())
                .createdAt(tc.getCreatedAt())
                .build();
    }
}

