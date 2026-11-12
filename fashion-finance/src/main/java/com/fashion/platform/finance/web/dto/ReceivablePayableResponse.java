package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.EntryType;
import com.fashion.platform.finance.domain.entity.PartyType;
import com.fashion.platform.finance.domain.entity.ReceivablePayable;
import com.fashion.platform.finance.domain.entity.ReceivableStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class ReceivablePayableResponse {
    UUID id;
    UUID legalEntityId;
    EntryType entryType;
    PartyType partyType;
    String partyId;
    String sourceType;
    String sourceId;
    BigDecimal originalAmount;
    BigDecimal settledAmount;
    BigDecimal remainingAmount;
    String currencyCode;
    LocalDate dueDate;
    ReceivableStatus status;
    Instant createdAt;
    Instant updatedAt;

    public static ReceivablePayableResponse from(ReceivablePayable rp) {
        return ReceivablePayableResponse.builder()
                .id(rp.getId())
                .legalEntityId(rp.getLegalEntity().getId())
                .entryType(rp.getEntryType())
                .partyType(rp.getPartyType())
                .partyId(rp.getPartyId())
                .sourceType(rp.getSourceType())
                .sourceId(rp.getSourceId())
                .originalAmount(rp.getOriginalAmount())
                .settledAmount(rp.getSettledAmount())
                .remainingAmount(rp.getRemainingAmount())
                .currencyCode(rp.getCurrencyCode())
                .dueDate(rp.getDueDate())
                .status(rp.getStatus())
                .createdAt(rp.getCreatedAt())
                .updatedAt(rp.getUpdatedAt())
                .build();
    }
}

