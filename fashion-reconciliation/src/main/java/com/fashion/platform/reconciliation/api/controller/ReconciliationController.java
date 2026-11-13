package com.fashion.platform.reconciliation.api.controller;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.reconciliation.api.dto.ReconciliationBatchResponse;
import com.fashion.platform.reconciliation.api.dto.ReconciliationItemResponse;
import com.fashion.platform.reconciliation.api.dto.ResolveItemRequest;
import com.fashion.platform.reconciliation.application.service.ReconciliationMatchingService;
import com.fashion.platform.reconciliation.application.service.ReconciliationResolutionService;
import com.fashion.platform.reconciliation.domain.model.ReconciliationBatch;
import com.fashion.platform.reconciliation.domain.model.ReconciliationItem;
import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationMatchingService matchingService;
    private final ReconciliationResolutionService resolutionService;
    private final ReconciliationBatchRepository batchRepository;
    private final ReconciliationItemRepository itemRepository;

    @PostMapping("/batches/upload")
    public ResponseEntity<ReconciliationBatchResponse> uploadFile(
            @RequestParam("legalEntityId") UUID legalEntityId,
            @RequestParam("sourceType") ReconciliationSourceType sourceType,
            @RequestParam("file") MultipartFile file) throws Exception {

        String currentUser = SecurityContextHelper.getCurrentUsername();
        ReconciliationBatch batch = matchingService.processSettlementFile(
                legalEntityId,
                sourceType,
                file.getOriginalFilename(),
                file.getBytes(),
                currentUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReconciliationBatchResponse.fromEntity(batch));
    }

    @PostMapping("/batches/upload-text")
    public ResponseEntity<ReconciliationBatchResponse> uploadText(
            @RequestParam("legalEntityId") UUID legalEntityId,
            @RequestParam("sourceType") ReconciliationSourceType sourceType,
            @RequestParam(value = "fileName", defaultValue = "settlement.csv") String fileName,
            @RequestBody String csvContent) {

        String currentUser = SecurityContextHelper.getCurrentUsername();
        ReconciliationBatch batch = matchingService.processSettlementFile(
                legalEntityId,
                sourceType,
                fileName,
                csvContent.getBytes(StandardCharsets.UTF_8),
                currentUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReconciliationBatchResponse.fromEntity(batch));
    }

    @GetMapping("/batches")
    public ResponseEntity<List<ReconciliationBatchResponse>> getBatches(
            @RequestParam("legalEntityId") UUID legalEntityId) {
        List<ReconciliationBatch> list = batchRepository.findByLegalEntityIdOrderByCreatedAtDesc(legalEntityId);
        return ResponseEntity.ok(list.stream().map(ReconciliationBatchResponse::fromEntity).toList());
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ReconciliationBatchResponse> getBatch(@PathVariable UUID id) {
        ReconciliationBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));
        return ResponseEntity.ok(ReconciliationBatchResponse.fromEntity(batch));
    }

    @GetMapping("/batches/{id}/items")
    public ResponseEntity<List<ReconciliationItemResponse>> getBatchItems(@PathVariable UUID id) {
        List<ReconciliationItem> items = itemRepository.findByBatchId(id);
        return ResponseEntity.ok(items.stream().map(ReconciliationItemResponse::fromEntity).toList());
    }

    @GetMapping("/batches/legal-entity/{legalEntityId}")
    public ResponseEntity<Page<ReconciliationBatchResponse>> getBatchesByLegalEntity(
            @PathVariable UUID legalEntityId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ReconciliationBatchResponse> page = batchRepository
                .findByLegalEntityIdOrderByCreatedAtDesc(legalEntityId, pageable)
                .map(ReconciliationBatchResponse::fromEntity);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/items/{itemId}/resolve")
    public ResponseEntity<ReconciliationItemResponse> resolveItem(
            @PathVariable UUID itemId,
            @RequestBody(required = false) ResolveItemRequest request) {

        String currentUser = SecurityContextHelper.getCurrentUsername();
        String notes = request != null && request.getNotes() != null ? request.getNotes() : "Đã chấp nhận chênh lệch và điều chỉnh phí";
        ReconciliationItem item = resolutionService.resolveDiscrepancy(itemId, notes, currentUser);
        return ResponseEntity.ok(ReconciliationItemResponse.fromEntity(item));
    }
}
