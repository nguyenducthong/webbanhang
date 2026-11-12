package com.fashion.platform.finance.web;

import com.fashion.platform.finance.service.CostService;
import com.fashion.platform.finance.web.dto.CostAllocationRequest;
import com.fashion.platform.finance.web.dto.CostCreateRequest;
import com.fashion.platform.finance.web.dto.CostResponse;
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
@RequestMapping("/api/v1/finance/costs")
@RequiredArgsConstructor
public class CostController {

    private final CostService costService;

    @PostMapping
    public ResponseEntity<CostResponse> create(
            @RequestParam UUID legalEntityId,
            @Valid @RequestBody CostCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costService.create(legalEntityId, request));
    }

    @PostMapping("/{costId}/allocate")
    public ResponseEntity<CostResponse> allocate(
            @RequestParam UUID legalEntityId,
            @PathVariable UUID costId,
            @Valid @RequestBody CostAllocationRequest request) {
        return ResponseEntity.ok(costService.allocate(legalEntityId, costId, request));
    }

    @GetMapping
    public ResponseEntity<Page<CostResponse>> list(
            @RequestParam UUID legalEntityId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(costService.list(legalEntityId, pageable));
    }
}

