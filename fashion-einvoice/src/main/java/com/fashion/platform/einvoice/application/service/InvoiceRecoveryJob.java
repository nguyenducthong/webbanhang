package com.fashion.platform.einvoice.application.service;

import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceStatus;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceRecoveryJob {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceIssuanceService issuanceService;

    @Scheduled(fixedDelay = 60000) // Chạy mỗi 1 phút
    public void autoRecoverPendingAndUnknownInvoices() {
        List<Invoice> stuckInvoices = invoiceRepository.findByInvoiceStatusIn(
                List.of(InvoiceStatus.UNKNOWN_RESULT, InvoiceStatus.SUBMITTED)
        );

        if (stuckInvoices.isEmpty()) {
            return;
        }

        log.info("Running InvoiceRecoveryJob: Found {} invoices needing status sync/recovery", stuckInvoices.size());

        for (Invoice invoice : stuckInvoices) {
            // Nếu SUBMITTED chưa quá 2 phút thì bỏ qua để chờ phản hồi bình thường
            if (invoice.getInvoiceStatus() == InvoiceStatus.SUBMITTED &&
                invoice.getUpdatedAt().isAfter(Instant.now().minus(2, ChronoUnit.MINUTES))) {
                continue;
            }

            try {
                log.info("Auto-recovering invoice {} in status {}", invoice.getId(), invoice.getInvoiceStatus());
                issuanceService.queryAndSyncStatus(invoice.getId(), "SYSTEM_RECOVERY_JOB");
            } catch (Exception e) {
                log.error("Failed to auto-recover invoice {}: {}", invoice.getId(), e.getMessage());
            }
        }
    }
}
