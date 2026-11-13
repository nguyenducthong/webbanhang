package com.fashion.platform.einvoice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fashion.platform.einvoice.api.dto.CreateInvoiceDraftRequest;
import com.fashion.platform.einvoice.api.dto.InvoiceItemDto;
import com.fashion.platform.einvoice.domain.model.*;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderAccountRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceRepository;
import com.fashion.platform.einvoice.domain.repository.InvoiceTemplateRepository;
import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.repository.FinanceTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDraftService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTemplateRepository invoiceTemplateRepository;
    private final InvoiceProviderAccountRepository providerAccountRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final FinanceTransactionRepository financeTransactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Invoice createDraft(CreateInvoiceDraftRequest request, String performedBy) {
        // 1. Kiểm tra tính Idempotent
        Optional<Invoice> existing = invoiceRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent hit: Invoice with idempotencyKey {} already exists (id={})",
                    request.getIdempotencyKey(), existing.get().getId());
            return existing.get();
        }

        // 2. Kiểm tra LegalEntity
        LegalEntity legalEntity = legalEntityRepository.findById(request.getLegalEntityId())
                .orElseThrow(() -> new IllegalArgumentException("Legal entity not found: " + request.getLegalEntityId()));

        if (legalEntity.getStatus() != LegalEntityStatus.ACTIVE) {
            throw new IllegalStateException("Legal entity is not ACTIVE: " + legalEntity.getEntityName());
        }

        // 3. Đóng gói SellerSnapshot
        SellerSnapshot sellerSnapshot = SellerSnapshot.builder()
                .legalName(legalEntity.getEntityName())
                .taxCode(legalEntity.getTaxCode())
                .address(legalEntity.getAddress())
                .build();

        // 4. Validate BuyerSnapshot
        BuyerSnapshot buyer = request.getBuyerSnapshot();
        if (buyer.isEnterprise()) {
            if (buyer.getTaxCode() == null || buyer.getTaxCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Enterprise buyer requires a valid taxCode");
            }
            if (buyer.getLegalName() == null || buyer.getLegalName().trim().isEmpty()) {
                throw new IllegalArgumentException("Enterprise buyer requires legalName");
            }
        }

        // 5. Tìm Template hóa đơn
        InvoiceTemplate template = invoiceTemplateRepository
                .findFirstByLegalEntityIdAndInvoiceTypeAndActiveTrue(legalEntity.getId(), request.getInvoiceType())
                .orElse(null);

        // 6. Tìm Provider Account
        InvoiceProviderAccount providerAccount = providerAccountRepository
                .findFirstByLegalEntityIdAndActiveTrue(legalEntity.getId())
                .orElse(null);

        // 7. Gắn FinanceTransaction nếu có
        FinanceTransaction tx = null;
        if (request.getFinanceTransactionId() != null) {
            tx = financeTransactionRepository.findById(request.getFinanceTransactionId()).orElse(null);
        }

        String sellerSnapshotJson = toJson(sellerSnapshot);
        String buyerSnapshotJson = toJson(buyer);

        Invoice invoice = Invoice.builder()
                .legalEntity(legalEntity)
                .providerAccount(providerAccount)
                .template(template)
                .financeTransaction(tx)
                .invoiceType(request.getInvoiceType())
                .invoiceStatus(InvoiceStatus.DRAFT)
                .idempotencyKey(request.getIdempotencyKey())
                .sellerSnapshot(sellerSnapshotJson)
                .buyerSnapshot(buyerSnapshotJson)
                .templateCode(template != null ? template.getTemplateCode() : null)
                .invoiceSeries(template != null ? template.getInvoiceSeries() : null)
                .notes(request.getNotes())
                .createdBy(performedBy)
                .updatedBy(performedBy)
                .build();

        // 8. Thêm items
        int lineNo = 1;
        for (InvoiceItemDto dto : request.getItems()) {
            InvoiceItem item = InvoiceItem.builder()
                    .lineNumber(dto.getLineNumber() > 0 ? dto.getLineNumber() : lineNo++)
                    .itemName(dto.getItemName())
                    .unit(dto.getUnit())
                    .quantity(dto.getQuantity())
                    .unitPrice(dto.getUnitPrice())
                    .discountAmount(dto.getDiscountAmount())
                    .taxRate(dto.getTaxRate())
                    .build();
            invoice.addItem(item);
        }

        invoice.transitionTo(InvoiceStatus.DRAFT, "CREATE_DRAFT", "Invoice draft created", performedBy);
        return invoiceRepository.save(invoice);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing snapshot to JSON", e);
        }
    }
}
