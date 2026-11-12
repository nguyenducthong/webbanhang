package com.fashion.platform.finance.web;

import com.fashion.platform.finance.domain.entity.TransactionChannel;
import com.fashion.platform.finance.service.FinanceTransactionService;
import com.fashion.platform.finance.web.dto.TransactionIngestRequest;
import com.fashion.platform.finance.web.dto.TransactionResponse;
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
@RequestMapping("/api/v1/finance/transactions")
@RequiredArgsConstructor
public class FinanceTransactionController {

    private final FinanceTransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> ingest(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody TransactionIngestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.ingest(legalEntityId, request));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
            @RequestParam UUID legalEntityId,
            @RequestParam(required = false) TransactionChannel channel,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.list(legalEntityId, channel, pageable));
    }
}

