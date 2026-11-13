package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.infrastructure.parser.CodCarrierSettlementCsvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodCarrierCsvParserTest {

    private final CodCarrierSettlementCsvParser parser = new CodCarrierSettlementCsvParser();

    @Test
    @DisplayName("Should correctly identify COD Carrier source type")
    void shouldSupportCodCarrierSource() {
        assertTrue(parser.supports(ReconciliationSourceType.COD));
        assertFalse(parser.supports(ReconciliationSourceType.SHOPEE));
        assertFalse(parser.supports(ReconciliationSourceType.TIKTOK));
    }

    @Test
    @DisplayName("Should parse standard COD Carrier GHN settlement CSV")
    void shouldParseCodCarrierCsv() {
        String csvData = """
                waybill_code,order_code,cod_amount,shipping_fee,insurance_fee,return_fee,net_payout
                GHN-VN-889922,FASHION-ORDER-001,850000,35000,5000,0,810000
                GHN-VN-889923,,300000,25000,0,0,275000
                """;

        InputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<ParsedTransaction> result = parser.parse(is);

        assertEquals(2, result.size());

        // Item 1: Ưu tiên order_code = FASHION-ORDER-001
        ParsedTransaction item1 = result.get(0);
        assertEquals("FASHION-ORDER-001", item1.orderId());
        assertEquals(new BigDecimal("850000"), item1.grossAmount());
        assertEquals(new BigDecimal("40000"), item1.feeAmount());
        assertEquals(new BigDecimal("810000"), item1.netAmount());

        // Item 2: Không có order_code thì lấy waybill_code = GHN-VN-889923
        ParsedTransaction item2 = result.get(1);
        assertEquals("GHN-VN-889923", item2.orderId());
        assertEquals(new BigDecimal("300000"), item2.grossAmount());
        assertEquals(new BigDecimal("25000"), item2.feeAmount());
        assertEquals(new BigDecimal("275000"), item2.netAmount());
    }
}
