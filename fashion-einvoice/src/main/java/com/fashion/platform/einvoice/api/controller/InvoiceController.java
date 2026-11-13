package com.fashion.platform.einvoice.api.controller;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.einvoice.api.dto.CreateInvoiceDraftRequest;
import com.fashion.platform.einvoice.api.dto.InvoiceResponse;
import com.fashion.platform.einvoice.application.service.InvoiceDraftService;
import com.fashion.platform.einvoice.application.service.InvoiceIssuanceService;
import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceDraftService draftService;
    private final InvoiceIssuanceService issuanceService;
    private final InvoiceRepository invoiceRepository;

    @PostMapping("/draft")
    public ResponseEntity<InvoiceResponse> createDraft(@Valid @RequestBody CreateInvoiceDraftRequest request) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Invoice invoice = draftService.createDraft(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponse.fromEntity(invoice));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<InvoiceResponse> validateInvoice(@PathVariable UUID id) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Invoice invoice = issuanceService.validateInvoice(id, currentUser);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @PostMapping("/{id}/issue")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable UUID id) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Invoice invoice = issuanceService.issueInvoice(id, currentUser);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @PostMapping("/{id}/sync-status")
    public ResponseEntity<InvoiceResponse> syncStatus(@PathVariable UUID id) {
        String currentUser = SecurityContextHelper.getCurrentUsername();
        Invoice invoice = issuanceService.queryAndSyncStatus(id, currentUser);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @GetMapping("/legal-entity/{legalEntityId}")
    public ResponseEntity<Page<InvoiceResponse>> getInvoicesByLegalEntity(
            @PathVariable UUID legalEntityId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<InvoiceResponse> page = invoiceRepository.findByLegalEntityId(legalEntityId, pageable)
                .map(InvoiceResponse::fromEntity);
        return ResponseEntity.ok(page);
    }
}
