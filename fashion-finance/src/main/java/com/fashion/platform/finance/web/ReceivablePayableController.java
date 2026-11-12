package com.fashion.platform.finance.web;

import com.fashion.platform.finance.domain.entity.PartyType;
import com.fashion.platform.finance.domain.entity.ReceivableStatus;
import com.fashion.platform.finance.service.ReceivablePayableService;
import com.fashion.platform.finance.web.dto.ReceivablePayableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/receivables-payables")
@RequiredArgsConstructor
public class ReceivablePayableController {

    private final ReceivablePayableService receivablePayableService;

    @GetMapping
    public ResponseEntity<Page<ReceivablePayableResponse>> list(
            @RequestParam UUID legalEntityId,
            @RequestParam(required = false) ReceivableStatus status,
            @RequestParam(required = false) PartyType partyType,
            @PageableDefault(size = 20) Pageable pageable) {
        if (partyType != null) {
            return ResponseEntity.ok(receivablePayableService.listByPartyType(legalEntityId, partyType, pageable));
        }
        return ResponseEntity.ok(receivablePayableService.listByStatus(legalEntityId, status, pageable));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<ReceivablePayableResponse> settle(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(receivablePayableService.settle(legalEntityId, id, amount));
    }
}

