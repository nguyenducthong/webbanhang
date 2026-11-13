package com.fashion.platform.reconciliation.infrastructure.parser;

import com.fashion.platform.reconciliation.domain.model.ReconciliationSourceType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser cho tệp sao kê ngân hàng điện tử (Vietcombank, VietinBank, BIDV, Techcombank).
 * Định dạng:
 * transaction_id, booking_date, debit_amount, credit_amount, balance, description
 */
@Component("bankStatementParser")
@Slf4j
public class BankStatementCsvParser implements ReconciliationParser {

    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(?i)(DH-[0-9A-Z-]+|ORD-[0-9A-Z-]+|TX-[0-9A-Z-]+|SP-[0-9A-Z-]+|TT-[0-9A-Z-]+)");

    @Override
    public boolean supports(ReconciliationSourceType type) {
        return type == ReconciliationSourceType.BANK;
    }

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

                String delimiter = line.contains(";") ? ";" : ",";
                String[] parts = line.split(delimiter);

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (parts.length < 4) {
                    log.warn("Skipping invalid Bank Statement CSV line: {}", line);
                    continue;
                }

                String txId = parts[0].trim().replace("\"", "");
                BigDecimal debit = parseDecimal(parts[2]);
                BigDecimal credit = parseDecimal(parts[3]);
                String desc = parts.length >= 6 ? parts[5].trim().replace("\"", "") : (parts.length >= 5 ? parts[4].trim().replace("\"", "") : "");

                // Tìm mã đơn hàng từ nội dung chuyển khoản VietQR / SMS Banking
                String matchId = txId;
                if (!desc.isEmpty()) {
                    Matcher matcher = ORDER_CODE_PATTERN.matcher(desc);
                    if (matcher.find()) {
                        matchId = matcher.group(1);
                    }
                }

                BigDecimal net = credit.compareTo(BigDecimal.ZERO) > 0 ? credit : debit.negate();
                results.add(new ParsedTransaction(matchId, credit, debit, net));
            }
        } catch (Exception e) {
            log.error("Failed to parse bank statement CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing bank statement CSV: " + e.getMessage(), e);
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
