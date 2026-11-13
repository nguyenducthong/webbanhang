package com.fashion.platform.finance.web;

import com.fashion.platform.finance.service.CashService;
import com.fashion.platform.finance.web.dto.CashDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping("/accounts")
    public ResponseEntity<CashDto.AccountResponse> createAccount(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody CashDto.AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashService.createAccount(legalEntityId, request));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<CashDto.AccountResponse>> listAccounts(@RequestParam UUID legalEntityId) {
        return ResponseEntity.ok(cashService.listActiveAccounts(legalEntityId));
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<CashDto.TransactionResponse> recordTransaction(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID accountId,
            @Valid @RequestBody CashDto.TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashService.recordTransaction(legalEntityId, accountId, request));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<Page<CashDto.TransactionResponse>> listTransactions(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cashService.listTransactions(accountId, pageable));
    }
}
