package com.fashion.platform.reconciliation;

import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.infrastructure.parser.ShopeeSettlementCsvParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CsvParserTest {

    @Test
    @DisplayName("Should successfully parse Shopee settlement CSV content")
    void shouldParseShopeeSettlementCsv() {
        String csvContent = "Order ID,Gross Sales,Platform Fee,Net Payout\n" +
                "SHOPEE-ORD-101,650000,65000,585000\n" +
                "SHOPEE-ORD-102,420000,42000,378000\n";

        ShopeeSettlementCsvParser parser = new ShopeeSettlementCsvParser();
        List<ParsedTransaction> list = parser.parse(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(list);
        assertEquals(2, list.size());

        ParsedTransaction item1 = list.get(0);
        assertEquals("SHOPEE-ORD-101", item1.orderId());
        assertEquals(0, new BigDecimal("650000").compareTo(item1.grossAmount()));
        assertEquals(0, new BigDecimal("65000").compareTo(item1.feeAmount()));
        assertEquals(0, new BigDecimal("585000").compareTo(item1.netAmount()));
    }
}
