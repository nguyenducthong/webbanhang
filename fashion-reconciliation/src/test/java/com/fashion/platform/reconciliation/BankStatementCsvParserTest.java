package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.infrastructure.parser.BankStatementCsvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankStatementCsvParserTest {

    private final BankStatementCsvParser parser = new BankStatementCsvParser();

    @Test
    @DisplayName("Should correctly identify BANK source type")
    void shouldSupportBankSource() {
        assertTrue(parser.supports(ReconciliationSourceType.BANK));
        assertFalse(parser.supports(ReconciliationSourceType.SHOPEE));
        assertFalse(parser.supports(ReconciliationSourceType.TIKTOK));
        assertFalse(parser.supports(ReconciliationSourceType.COD));
    }

    @Test
    @DisplayName("Should parse bank statement CSV and extract internal order code from description")
    void shouldParseBankStatementAndExtractOrderCode() {
        String csvData = """
                transaction_id,booking_date,debit_amount,credit_amount,balance,description
                FT26111388991,2026-11-13 09:15:00,0,1250000,50000000,NGUYEN VAN A CHUYEN TIEN DH-20261113-888
                FT26111388992,2026-11-13 10:20:00,0,850000,50850000,VIETQR THANH TOAN DON HANG SP-20261113-001
                FT26111388993,2026-11-13 11:00:00,5500,0,50844500,PHI DUY TRI TAI KHOAN DOANH NGHIEP
                """;

        InputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<ParsedTransaction> results = parser.parse(is);

        assertEquals(3, results.size());

        // Row 1: trích xuất mã đơn DH-20261113-888 từ description
        ParsedTransaction row1 = results.get(0);
        assertEquals("DH-20261113-888", row1.orderId());
        assertEquals(new BigDecimal("1250000"), row1.grossAmount());
        assertEquals(BigDecimal.ZERO, row1.feeAmount());
        assertEquals(new BigDecimal("1250000"), row1.netAmount());

        // Row 2: trích xuất mã đơn SP-20261113-001 từ description
        ParsedTransaction row2 = results.get(1);
        assertEquals("SP-20261113-001", row2.orderId());
        assertEquals(new BigDecimal("850000"), row2.grossAmount());
        assertEquals(new BigDecimal("850000"), row2.netAmount());

        // Row 3: không có pattern mã đơn, lấy fallback transaction_id
        ParsedTransaction row3 = results.get(2);
        assertEquals("FT26111388993", row3.orderId());
        assertEquals(BigDecimal.ZERO, row3.grossAmount());
        assertEquals(new BigDecimal("5500"), row3.feeAmount());
    }
}
