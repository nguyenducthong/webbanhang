package com.fashion.platform.reconciliation.infrastructure.parser;

import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.domain.parser.ReconciliationParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component("shopeeSettlementParser")
@Slf4j
public class ShopeeSettlementCsvParser implements ReconciliationParser {

    @Override
    public List<ParsedTransaction> parse(InputStream inputStream) {
        List<ParsedTransaction> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Tách theo dấu phẩy hoặc chấm phẩy
                String delimiter = line.contains(";") ? ";" : ",";
                String[] parts = line.split(delimiter);

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (parts.length < 3) {
                    log.warn("Skipping invalid CSV line: {}", line);
                    continue;
                }

                String orderId = parts[0].trim().replace("\"", "");
                BigDecimal gross = parseDecimal(parts[1]);
                BigDecimal fee = parseDecimal(parts[2]);
                BigDecimal net = parts.length >= 4 ? parseDecimal(parts[3]) : gross.subtract(fee);

                results.add(new ParsedTransaction(orderId, gross, fee, net));
            }
        } catch (Exception e) {
            log.error("Failed to parse settlement CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing Shopee settlement CSV: " + e.getMessage(), e);
        }
        return results;
    }

    private BigDecimal parseDecimal(String val) {
        if (val == null) return BigDecimal.ZERO;
        String clean = val.trim().replace("\"", "").replace(" ", "").replace(",", "");
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
