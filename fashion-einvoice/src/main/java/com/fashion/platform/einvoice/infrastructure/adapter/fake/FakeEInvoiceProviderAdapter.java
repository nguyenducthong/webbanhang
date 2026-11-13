package com.fashion.platform.einvoice.infrastructure.adapter.fake;

import com.fashion.platform.einvoice.domain.adapter.EInvoiceProviderAdapter;
import com.fashion.platform.einvoice.domain.adapter.ProviderIssuanceResult;
import com.fashion.platform.einvoice.domain.adapter.ProviderStatusResult;
import com.fashion.platform.einvoice.domain.model.Invoice;
import com.fashion.platform.einvoice.domain.model.InvoiceDocumentType;
import com.fashion.platform.einvoice.domain.model.InvoiceProviderAccount;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FakeEInvoiceProviderAdapter implements EInvoiceProviderAdapter {

    private final AtomicInteger invoiceSeq = new AtomicInteger(1);
    private final Map<String, ProviderIssuanceResult> issuedStore = new ConcurrentHashMap<>();
    private volatile boolean simulateTimeout = false;

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    @Override
    public String getProviderCode() {
        return "FAKE";
    }

    @Override
    public ProviderIssuanceResult issueInvoice(Invoice invoice, InvoiceProviderAccount account) {
        String key = invoice.getIdempotencyKey();

        // Nếu đã từng phát hành thành công với key này thì trả lại kết quả cũ (Idempotent)
        if (issuedStore.containsKey(key)) {
            return issuedStore.get(key);
        }

        // Nếu đang bật chế độ mô phỏng timeout
        if (simulateTimeout) {
            // Giả lập phát hành ngầm tại NCC nhưng client bị ngắt mạng / timeout
            String invoiceNumber = String.format("%07d", invoiceSeq.getAndIncrement());
            String taxCode = "TCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String series = invoice.getTemplate() != null ? invoice.getTemplate().getInvoiceSeries() : "C26TDD";
            String templateCode = invoice.getTemplate() != null ? invoice.getTemplate().getTemplateCode() : "1/001";
            Instant now = Instant.now();

            String xml = generateFakeXml(invoice, invoiceNumber, taxCode);
            byte[] pdf = generateFakePdf(invoice, invoiceNumber);

            ProviderIssuanceResult realResult = ProviderIssuanceResult.success(
                    invoiceNumber, series, templateCode, now, taxCode, xml, pdf
            );
            // Lưu vào kho của NCC
            issuedStore.put(key, realResult);

            // Nhưng trả về Unknown Result cho caller vì bị đứt kết nối mạng giữa chừng
            return ProviderIssuanceResult.unknown("Connection timed out after 30000ms while waiting for response from E-Invoice Provider");
        }

        // Phát hành bình thường
        String invoiceNumber = String.format("%07d", invoiceSeq.getAndIncrement());
        String taxCode = "TCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String series = invoice.getTemplate() != null ? invoice.getTemplate().getInvoiceSeries() : "C26TDD";
        String templateCode = invoice.getTemplate() != null ? invoice.getTemplate().getTemplateCode() : "1/001";
        Instant now = Instant.now();

        String xml = generateFakeXml(invoice, invoiceNumber, taxCode);
        byte[] pdf = generateFakePdf(invoice, invoiceNumber);

        ProviderIssuanceResult result = ProviderIssuanceResult.success(
                invoiceNumber, series, templateCode, now, taxCode, xml, pdf
        );
        issuedStore.put(key, result);
        return result;
    }

    @Override
    public ProviderStatusResult queryInvoiceStatus(Invoice invoice, InvoiceProviderAccount account) {
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

    private String generateFakeXml(Invoice invoice, String invoiceNumber, String taxAuthorityCode) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<HDon>\n" +
                "  <DLHDon Id=\"Invoice_" + invoice.getId() + "\">\n" +
                "    <TTChung>\n" +
                "      <KHMSHDon>" + (invoice.getTemplate() != null ? invoice.getTemplate().getTemplateCode() : "1/001") + "</KHMSHDon>\n" +
                "      <KHHDon>" + (invoice.getTemplate() != null ? invoice.getTemplate().getInvoiceSeries() : "C26TDD") + "</KHHDon>\n" +
                "      <SHDon>" + invoiceNumber + "</SHDon>\n" +
                "      <NLap>" + Instant.now() + "</NLap>\n" +
                "      <DVTTe>" + invoice.getCurrency() + "</DVTTe>\n" +
                "      <MCCQT>" + taxAuthorityCode + "</MCCQT>\n" +
                "    </TTChung>\n" +
                "    <NDHDon>\n" +
                "      <NBan>\n" +
                "        <SellerSnapshot>" + invoice.getSellerSnapshot() + "</SellerSnapshot>\n" +
                "      </NBan>\n" +
                "      <NMua>\n" +
                "        <BuyerSnapshot>" + invoice.getBuyerSnapshot() + "</BuyerSnapshot>\n" +
                "      </NMua>\n" +
                "      <TToan>\n" +
                "        <TgTTTBSo>" + invoice.getTotalPretaxAmount() + "</TgTTTBSo>\n" +
                "        <TgTThue>" + invoice.getTotalTaxAmount() + "</TgTThue>\n" +
                "        <TgTTSo>" + invoice.getTotalAmount() + "</TgTTSo>\n" +
                "      </TToan>\n" +
                "    </NDHDon>\n" +
                "  </DLHDon>\n" +
                "  <DSCKS>\n" +
                "    <Signature>FAKED_DIGITAL_SIGNATURE_SHA256_RSA</Signature>\n" +
                "  </DSCKS>\n" +
                "</HDon>";
    }

    private byte[] generateFakePdf(Invoice invoice, String invoiceNumber) {
        String pdfContent = "%PDF-1.4\n" +
                "% FAKE E-INVOICE PDF FOR INVOICE " + invoiceNumber + "\n" +
                "Buyer: " + invoice.getBuyerSnapshot() + "\n" +
                "Total: " + invoice.getTotalAmount() + " " + invoice.getCurrency() + "\n" +
                "%%EOF";
        return pdfContent.getBytes(StandardCharsets.UTF_8);
    }
}
