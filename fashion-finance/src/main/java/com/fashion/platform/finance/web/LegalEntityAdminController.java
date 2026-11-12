package com.fashion.platform.finance.web;

import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.service.LegalEntityService;
import com.fashion.platform.finance.web.dto.LegalEntityCreateRequest;
import com.fashion.platform.finance.web.dto.LegalEntityResponse;
import com.fashion.platform.finance.web.dto.LegalEntityUpdateRequest;
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
@RequestMapping("/api/v1/admin/legal-entities")
@RequiredArgsConstructor
public class LegalEntityAdminController {

    private final LegalEntityService legalEntityService;

    @PostMapping
    public ResponseEntity<LegalEntityResponse> create(@Valid @RequestBody LegalEntityCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legalEntityService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<LegalEntityResponse>> list(
            @RequestParam(required = false) LegalEntityStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(legalEntityService.listAll(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(legalEntityService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LegalEntityResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LegalEntityUpdateRequest request) {
        return ResponseEntity.ok(legalEntityService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LegalEntityResponse> changeStatus(
            @PathVariable UUID id,
            @RequestParam LegalEntityStatus status) {
        return ResponseEntity.ok(legalEntityService.changeStatus(id, status));
    }
}
