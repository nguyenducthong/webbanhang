package com.fashion.platform.finance.web;

import com.fashion.platform.finance.service.AccountingAccountService;
import com.fashion.platform.finance.web.dto.AccountingAccountDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finance/accounts")
@RequiredArgsConstructor
public class AccountingAccountController {

    private final AccountingAccountService accountService;

    @PostMapping
    public ResponseEntity<AccountingAccountDto.Response> create(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody AccountingAccountDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(legalEntityId, request));
    }

    @GetMapping
    public ResponseEntity<List<AccountingAccountDto.Response>> list(@RequestParam UUID legalEntityId) {
        return ResponseEntity.ok(accountService.listActiveAccounts(legalEntityId));
    }

    @PostMapping("/seed-standard")
    public ResponseEntity<Void> seedStandardCOA(@RequestParam UUID legalEntityId) {
        accountService.seedStandardChartOfAccounts(legalEntityId);
        return ResponseEntity.ok().build();
    }
}
