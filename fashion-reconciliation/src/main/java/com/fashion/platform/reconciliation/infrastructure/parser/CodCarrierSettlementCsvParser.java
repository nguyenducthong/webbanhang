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

/**
 * Parser cho tệp sao kê quyết toán đơn vị vận chuyển COD (GHN / ViettelPost / J&T).
 * Định dạng:
 * Waybill Code, Order Code, COD Amount, Shipping Fee, Insurance Fee, Return Fee, Net Payout
 */
@Component("codCarrierSettlementParser")
@Slf4j
public class CodCarrierSettlementCsvParser implements ReconciliationParser {

    @Override
    public boolean supports(ReconciliationSourceType type) {
        return type == ReconciliationSourceType.COD;
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

                if (parts.length < 3) {
                    log.warn("Skipping invalid COD CSV line: {}", line);
                    continue;
                }

                String waybillCode = parts[0].trim().replace("\"", "");
                String orderCode = parts[1].trim().replace("\"", "");
                String matchId = !orderCode.isEmpty() ? orderCode : waybillCode;

                if (parts.length >= 7) {
                    BigDecimal codAmount = parseDecimal(parts[2]);
                    BigDecimal shippingFee = parseDecimal(parts[3]);
                    BigDecimal insuranceFee = parseDecimal(parts[4]);
                    BigDecimal returnFee = parseDecimal(parts[5]);
                    BigDecimal netPayout = parseDecimal(parts[6]);

                    BigDecimal totalFee = shippingFee.add(insuranceFee).add(returnFee);
                    results.add(new ParsedTransaction(matchId, codAmount, totalFee, netPayout));
                } else if (parts.length >= 4) {
                    // Cột: waybill, order_code, cod_amount, net_payout
                    BigDecimal codAmount = parseDecimal(parts[2]);
                    BigDecimal netPayout = parseDecimal(parts[3]);
                    BigDecimal fee = codAmount.subtract(netPayout).abs();
                    results.add(new ParsedTransaction(matchId, codAmount, fee, netPayout));
                } else {
                    // Dạng tối giản: match_id, payout, fee
                    BigDecimal payout = parseDecimal(parts[1]);
                    BigDecimal fee = parseDecimal(parts[2]);
                    BigDecimal gross = payout.add(fee);
                    results.add(new ParsedTransaction(matchId, gross, fee, payout));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse COD carrier settlement CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing COD carrier settlement CSV: " + e.getMessage(), e);
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
