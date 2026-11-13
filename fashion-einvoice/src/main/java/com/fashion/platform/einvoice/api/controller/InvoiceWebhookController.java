package com.fashion.platform.einvoice.api.controller;

import com.fashion.platform.einvoice.application.service.InvoiceWebhookService;
import com.fashion.platform.einvoice.domain.model.InvoiceWebhookLog;
import com.fashion.platform.einvoice.domain.repository.InvoiceWebhookLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices/webhook")
@RequiredArgsConstructor
public class InvoiceWebhookController {

    private final InvoiceWebhookService webhookService;
    private final InvoiceWebhookLogRepository webhookLogRepository;

    @PostMapping("/{providerCode}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String providerCode,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String payload) {

        webhookService.processWebhook(providerCode, payload, signature);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Webhook received and processed successfully"
        ));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getRecentWebhookLogs() {
        List<Map<String, Object>> list = webhookLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(l -> Map.<String, Object>of(
                        "id", l.getId().toString(),
                        "providerCode", l.getProviderCode(),
                        "invoiceId", l.getInvoice() != null ? l.getInvoice().getId().toString() : "",
                        "eventType", l.getEventType(),
                        "processedStatus", l.getProcessedStatus(),
                        "signature", l.getSignature() != null ? l.getSignature() : "",
                        "createdAt", l.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }
}
