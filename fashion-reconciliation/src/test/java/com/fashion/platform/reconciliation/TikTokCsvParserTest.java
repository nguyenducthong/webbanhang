package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.infrastructure.parser.TikTokSettlementCsvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TikTokCsvParserTest {

    private final TikTokSettlementCsvParser parser = new TikTokSettlementCsvParser();

    @Test
    @DisplayName("Should correctly identify TikTok source type")
    void shouldSupportTikTokSource() {
        assertTrue(parser.supports(ReconciliationSourceType.TIKTOK));
        assertFalse(parser.supports(ReconciliationSourceType.SHOPEE));
        assertFalse(parser.supports(ReconciliationSourceType.COD));
    }

    @Test
    @DisplayName("Should parse standard multi-component TikTok Shop settlement CSV")
    void shouldParseMultiComponentTikTokCsv() {
        String csvData = """
                order_id,customer_payment,platform_voucher,seller_voucher,commission_fee,transaction_fee,affiliate_fee,settlement_amount
                TT-20261113-001,700000,50000,30000,35000,17500,20000,647500
                TT-20261113-002,400000,0,10000,20000,10000,0,360000
                """;

        InputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<ParsedTransaction> result = parser.parse(is);

        assertEquals(2, result.size());

        // Item 1: customer_payment 700k + platform_voucher 50k = gross 750k
        // fee = commission 35k + trans 17.5k + affiliate 20k = 72.5k
        // settlement = 647.5k
        ParsedTransaction item1 = result.get(0);
        assertEquals("TT-20261113-001", item1.orderId());
        assertEquals(new BigDecimal("750000"), item1.grossAmount());
        assertEquals(new BigDecimal("72500"), item1.feeAmount());
        assertEquals(new BigDecimal("647500"), item1.netAmount());

        // Item 2
        ParsedTransaction item2 = result.get(1);
        assertEquals("TT-20261113-002", item2.orderId());
        assertEquals(new BigDecimal("400000"), item2.grossAmount());
        assertEquals(new BigDecimal("30000"), item2.feeAmount());
        assertEquals(new BigDecimal("360000"), item2.netAmount());
    }
}
