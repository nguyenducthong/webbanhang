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
 * Parser cho tệp sao kê quyết toán TikTok Shop.
 * Định dạng cột phổ biến:
 * Order ID, Customer Payment, Platform Voucher, Seller Voucher, Commission Fee, Payment Fee, Affiliate Fee, Settlement Amount
 */
@Component("tikTokSettlementParser")
@Slf4j
public class TikTokSettlementCsvParser implements ReconciliationParser {

    @Override
    public boolean supports(ReconciliationSourceType type) {
        return type == ReconciliationSourceType.TIKTOK;
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

                if (parts.length < 2) {
                    log.warn("Skipping invalid TikTok CSV line: {}", line);
                    continue;
                }

                String orderId = parts[0].trim().replace("\"", "");

                if (parts.length >= 8) {
                    BigDecimal customerPayment = parseDecimal(parts[1]);
                    BigDecimal platformVoucher = parseDecimal(parts[2]);
                    BigDecimal sellerVoucher = parseDecimal(parts[3]);
                    BigDecimal commissionFee = parseDecimal(parts[4]);
                    BigDecimal paymentFee = parseDecimal(parts[5]);
                    BigDecimal affiliateFee = parseDecimal(parts[6]);
                    BigDecimal settlementAmount = parseDecimal(parts[7]);

                    BigDecimal gross = customerPayment.add(platformVoucher);
                    BigDecimal totalFee = commissionFee.add(paymentFee).add(affiliateFee);

                    results.add(new ParsedTransaction(orderId, gross, totalFee, settlementAmount));
                } else if (parts.length >= 3) {
                    // Dạng rút gọn: order_id, payout_amount, fee
                    BigDecimal payout = parseDecimal(parts[1]);
                    BigDecimal fee = parseDecimal(parts[2]);
                    BigDecimal gross = payout.add(fee);
                    results.add(new ParsedTransaction(orderId, gross, fee, payout));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse TikTok settlement CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing TikTok settlement CSV: " + e.getMessage(), e);
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
