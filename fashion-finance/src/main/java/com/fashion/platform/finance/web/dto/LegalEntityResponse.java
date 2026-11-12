package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.domain.entity.LegalEntityType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class LegalEntityResponse {
    UUID id;
    String entityCode;
    String entityName;
    LegalEntityType entityType;
    String taxCode;
    String businessRegistrationNo;
    String address;
    String countryCode;
    LegalEntityStatus status;
    Instant createdAt;
    Instant updatedAt;

    public static LegalEntityResponse from(LegalEntity entity) {
        return LegalEntityResponse.builder()
                .id(entity.getId())
                .entityCode(entity.getEntityCode())
                .entityName(entity.getEntityName())
                .entityType(entity.getEntityType())
                .taxCode(entity.getTaxCode())
                .businessRegistrationNo(entity.getBusinessRegistrationNo())
                .address(entity.getAddress())
                .countryCode(entity.getCountryCode())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
