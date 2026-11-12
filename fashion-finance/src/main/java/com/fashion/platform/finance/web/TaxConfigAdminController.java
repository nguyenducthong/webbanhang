package com.fashion.platform.finance.web;

import com.fashion.platform.finance.domain.entity.TaxType;
import com.fashion.platform.finance.service.TaxConfigurationService;
import com.fashion.platform.finance.web.dto.TaxConfigCreateRequest;
import com.fashion.platform.finance.web.dto.TaxConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/legal-entities/{entityId}/tax-configs")
@RequiredArgsConstructor
public class TaxConfigAdminController {

    private final TaxConfigurationService taxConfigurationService;

    @PostMapping
    public ResponseEntity<TaxConfigResponse> create(
            @PathVariable UUID entityId,
            @Valid @RequestBody TaxConfigCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxConfigurationService.create(entityId, request));
    }

    @GetMapping
    public ResponseEntity<List<TaxConfigResponse>> list(@PathVariable UUID entityId) {
        return ResponseEntity.ok(taxConfigurationService.listByLegalEntity(entityId));
    }

    @GetMapping("/effective")
    public ResponseEntity<TaxConfigResponse> getEffective(
            @PathVariable UUID entityId,
            @RequestParam TaxType taxType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TaxConfigResponse response = taxConfigurationService.findEffectiveTaxConfig(entityId, taxType, date);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{configId}/approve")
    public ResponseEntity<TaxConfigResponse> approve(
            @PathVariable UUID entityId,
            @PathVariable UUID configId) {
        return ResponseEntity.ok(taxConfigurationService.approve(entityId, configId));
    }
}

