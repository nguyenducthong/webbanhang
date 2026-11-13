package com.fashion.platform.einvoice;

import com.fashion.platform.einvoice.application.service.InvoiceIssuanceService;
import com.fashion.platform.einvoice.application.service.InvoiceRecoveryJob;
import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceStatus;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BackgroundRecoveryJobTest {

    @Test
    @DisplayName("Should trigger queryAndSyncStatus for stuck UNKNOWN_RESULT invoice")
    void shouldTriggerRecoveryForUnknownInvoice() {
        InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
        InvoiceIssuanceService issuanceService = Mockito.mock(InvoiceIssuanceService.class);

        InvoiceRecoveryJob job = new InvoiceRecoveryJob(invoiceRepository, issuanceService);

        UUID invoiceId = UUID.randomUUID();
        Invoice stuckInvoice = Invoice.builder()
                .id(invoiceId)
                .invoiceStatus(InvoiceStatus.UNKNOWN_RESULT)
                .updatedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        when(invoiceRepository.findByInvoiceStatusIn(any())).thenReturn(List.of(stuckInvoice));

        job.autoRecoverPendingAndUnknownInvoices();

        verify(issuanceService, times(1)).queryAndSyncStatus(eq(invoiceId), eq("SYSTEM_RECOVERY_JOB"));
    }
}
