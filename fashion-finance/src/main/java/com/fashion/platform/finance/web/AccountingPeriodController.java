package com.fashion.platform.finance.web;

import com.fashion.platform.finance.service.AccountingPeriodService;
import com.fashion.platform.finance.web.dto.AccountingPeriodDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService periodService;

    @PostMapping
    public ResponseEntity<AccountingPeriodDto.Response> create(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody AccountingPeriodDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodService.create(legalEntityId, request));
    }

    @PostMapping("/{periodId}/close")
    public ResponseEntity<AccountingPeriodDto.Response> close(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(periodService.closePeriod(legalEntityId, periodId));
    }

    @PostMapping("/{periodId}/reopen")
    public ResponseEntity<AccountingPeriodDto.Response> reopen(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID periodId) {
        return ResponseEntity.ok(periodService.reopenPeriod(legalEntityId, periodId));
    }

    @GetMapping
    public ResponseEntity<Page<AccountingPeriodDto.Response>> list(
            @RequestParam UUID legalEntityId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(periodService.list(legalEntityId, pageable));
    }
}
