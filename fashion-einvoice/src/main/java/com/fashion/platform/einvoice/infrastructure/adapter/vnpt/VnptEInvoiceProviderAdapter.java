package com.fashion.platform.einvoice.infrastructure.adapter.vnpt;

import com.fashion.platform.einvoice.domain.adapter.EInvoiceProviderAdapter;
import com.fashion.platform.einvoice.domain.adapter.ProviderIssuanceResult;
import com.fashion.platform.einvoice.domain.adapter.ProviderStatusResult;
import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceDocumentType;
import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;
import com.fashion.platform.einvoice.domain.repository.InvoiceProviderAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class VnptEInvoiceProviderAdapter implements EInvoiceProviderAdapter {

    private final InvoiceProviderAccountRepository accountRepository;
    private final AtomicInteger invoiceSeq = new AtomicInteger(100);
    private final Map<String, ProviderIssuanceResult> issuedStore = new ConcurrentHashMap<>();

    @Override
    public String getProviderCode() {
        return "VNPT";
    }

    /**
     * Quản lý lấy hoặc tái sử dụng Authentication Token từ cache
     */
    public synchronized String getOrRefreshToken(InvoiceProviderAccount account) {
        if (account == null) return "mock_token_vnpt";

        if (account.getAuthTokenCached() != null &&
            account.getTokenExpiry() != null &&
            account.getTokenExpiry().isAfter(Instant.now())) {
            log.debug("Reusing cached VNPT token for account: {}", account.getAccountUsername());
            return account.getAuthTokenCached();
        }

        // Tạo token mới giả lập xác thực API OAuth2 của VNPT
        String newToken = "vnpt_token_" + UUID.randomUUID();
        account.setAuthTokenCached(newToken);
        account.setTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        accountRepository.save(account);
        log.info("Refreshed new authentication token for VNPT provider account: {}", account.getAccountUsername());
        return newToken;
    }

    @Override
    public ProviderIssuanceResult issueInvoice(Invoice invoice, InvoiceProviderAccount account) {
        String token = getOrRefreshToken(account);
        String key = invoice.getIdempotencyKey();

        if (issuedStore.containsKey(key)) {
            log.info("VNPT Idempotent hit for key: {}", key);
            return issuedStore.get(key);
        }

        String invoiceNumber = String.format("%07d", invoiceSeq.getAndIncrement());
        String series = invoice.getTemplate() != null ? invoice.getTemplate().getInvoiceSeries() : "C26TDD";
        String templateCode = invoice.getTemplate() != null ? invoice.getTemplate().getTemplateCode() : "1/001";
        Instant now = Instant.now();
        String taxCode = "TCT-VNPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String xml = buildSignedVnptXml(invoice, invoiceNumber, series, templateCode, taxCode);
        byte[] pdf = buildVnptPdf(invoice, invoiceNumber);

        ProviderIssuanceResult result = ProviderIssuanceResult.success(
                invoiceNumber, series, templateCode, now, taxCode, xml, pdf
        );
        issuedStore.put(key, result);
        log.info("Successfully issued VNPT Invoice #{} for idempotencyKey {}", invoiceNumber, key);
        return result;
    }

    @Override
    public ProviderStatusResult queryInvoiceStatus(Invoice invoice, InvoiceProviderAccount account) {
        getOrRefreshToken(account);
        String key = invoice.getIdempotencyKey();
        ProviderIssuanceResult existing = issuedStore.get(key);
        if (existing != null && existing.success()) {
            return ProviderStatusResult.issued(existing.invoiceNumber(), existing.taxAuthorityCode(), existing.issueDate());
        }
        return ProviderStatusResult.notFound();
    }

    @Override
    public byte[] downloadDocument(Invoice invoice, InvoiceProviderAccount account, InvoiceDocumentType documentType) {
        String key = invoice.getIdempotencyKey();
        ProviderIssuanceResult existing = issuedStore.get(key);
        if (existing == null) {
            return new byte[0];
        }
        if (documentType == InvoiceDocumentType.XML_ORIGINAL) {
            return existing.xmlContent().getBytes(StandardCharsets.UTF_8);
        } else {
            return existing.pdfContent();
        }
    }

    private String buildSignedVnptXml(Invoice invoice, String invoiceNumber, String series, String templateCode, String taxAuthorityCode) {
        String signatureHash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((invoice.getId() + invoiceNumber + taxAuthorityCode).getBytes(StandardCharsets.UTF_8));
            signatureHash = HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            signatureHash = "MOCK_SIGNATURE_HASH";
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<HDon xmlns=\"http://laphoadon.gdt.gov.vn/2020/01/tt78\">\n" +
                "  <DLHDon Id=\"VNPT_" + invoice.getId() + "\">\n" +
                "    <TTChung>\n" +
                "      <PBan>2.0.0</PBan>\n" +
                "      <KHMSHDon>" + templateCode + "</KHMSHDon>\n" +
                "      <KHHDon>" + series + "</KHHDon>\n" +
                "      <SHDon>" + invoiceNumber + "</SHDon>\n" +
                "      <NLap>" + Instant.now() + "</NLap>\n" +
                "      <DVTTe>" + invoice.getCurrency() + "</DVTTe>\n" +
                "      <MCCQT>" + taxAuthorityCode + "</MCCQT>\n" +
                "    </TTChung>\n" +
                "    <NDHDon>\n" +
                "      <NBan>\n" +
                "        <Snapshot>" + invoice.getSellerSnapshot() + "</Snapshot>\n" +
                "      </NBan>\n" +
                "      <NMua>\n" +
                "        <Snapshot>" + invoice.getBuyerSnapshot() + "</Snapshot>\n" +
                "      </NMua>\n" +
                "      <TToan>\n" +
                "        <TgTTTBSo>" + invoice.getTotalPretaxAmount() + "</TgTTTBSo>\n" +
                "        <TgTThue>" + invoice.getTotalTaxAmount() + "</TgTThue>\n" +
                "        <TgTTSo>" + invoice.getTotalAmount() + "</TgTTSo>\n" +
                "      </TToan>\n" +
                "    </NDHDon>\n" +
                "  </DLHDon>\n" +
                "  <DSCKS>\n" +
                "    <Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                "      <SignedInfo>\n" +
                "        <CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>\n" +
                "        <SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha256\"/>\n" +
                "        <Reference URI=\"#VNPT_" + invoice.getId() + "\">\n" +
                "          <DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha256\"/>\n" +
                "          <DigestValue>" + signatureHash + "</DigestValue>\n" +
                "        </Reference>\n" +
                "      </SignedInfo>\n" +
                "      <SignatureValue>VNPT_DIGITAL_SIGNATURE_BASE64_" + signatureHash + "</SignatureValue>\n" +
                "    </Signature>\n" +
                "  </DSCKS>\n" +
                "</HDon>";
    }

    private byte[] buildVnptPdf(Invoice invoice, String invoiceNumber) {
        String pdf = "%PDF-1.4\n" +
                "% VNPT ELECTRONIC INVOICE REPRESENTATION\n" +
                "InvoiceNumber: " + invoiceNumber + "\n" +
                "Buyer: " + invoice.getBuyerSnapshot() + "\n" +
                "Total: " + invoice.getTotalAmount() + " " + invoice.getCurrency() + "\n" +
                "%%EOF";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }
}
