package com.fashion.platform.einvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.application.service.InvoiceIssuanceService;
import com.fashion.platform.einvoice.domain.adapter.EInvoiceProviderAdapter;
import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.einvoice.domain.repository.InvoiceDocumentRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DuplicateIssuanceProtectionTest {

    @Test
    @DisplayName("Should block duplicate issuance call when invoice is already ISSUED")
    void shouldBlockDuplicateIssuanceOnAlreadyIssuedInvoice() {
        InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
        InvoiceDocumentRepository documentRepository = Mockito.mock(InvoiceDocumentRepository.class);
        EInvoiceProviderAdapter adapter = Mockito.mock(EInvoiceProviderAdapter.class);
        when(adapter.getProviderCode()).thenReturn("VNPT");

        InvoiceIssuanceService service = new InvoiceIssuanceService(
                invoiceRepository,
                documentRepository,
                List.of(adapter),
                new ObjectMapper()
        );

        UUID invoiceId = UUID.randomUUID();
        Invoice issuedInvoice = Invoice.builder()
                .id(invoiceId)
                .invoiceStatus(InvoiceStatus.ISSUED)
                .invoiceNumber("0000088")
                .taxAuthorityCode("TCT-VNPT-EXISTING")
                .build();

        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(issuedInvoice));

        Invoice result = service.issueInvoice(invoiceId, "tester");

        assertEquals(InvoiceStatus.ISSUED, result.getInvoiceStatus());
        assertEquals("0000088", result.getInvoiceNumber());

        // Đảm bảo tuyệt đối không gọi adapter.issueInvoice()
        verify(adapter, never()).issueInvoice(any(), any());
    }
}
