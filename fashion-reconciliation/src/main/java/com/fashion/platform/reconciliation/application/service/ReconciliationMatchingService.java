package com.fashion.platform.reconciliation.application.service;

import com.fashion.platform.finance.domain.entity.FinanceTransaction;
import com.fashion.platform.finance.domain.entity.FinanceTransactionLine;
import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.repository.FinanceTransactionRepository;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.reconciliation.domain.model.*;
import com.fashion.platform.reconciliation.domain.parser.ParsedTransaction;
import com.fashion.platform.reconciliation.domain.parser.ReconciliationParser;
import com.fashion.platform.reconciliation.domain.repository.ReconciliationBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationMatchingService {

    private final ReconciliationBatchRepository batchRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final FinanceTransactionRepository financeTransactionRepository;
    private final ReconciliationParser shopeeSettlementParser;

    @Transactional
    public ReconciliationBatch processSettlementFile(
            UUID legalEntityId,
            ReconciliationSourceType sourceType,
            String fileName,
            byte[] fileBytes,
            String performedBy
    ) {
        log.info("Starting reconciliation settlement process for entity {}, file {}", legalEntityId, fileName);

        LegalEntity legalEntity = legalEntityRepository.findById(legalEntityId)
                .orElseThrow(() -> new IllegalArgumentException("Legal entity not found: " + legalEntityId));

        // 1. Tính hash SHA-256 của file bằng chứng
        String fileHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            fileHash = HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            fileHash = "MOCK_HASH";
        }

        String batchCode = "BATCH-" + sourceType.name() + "-" + System.currentTimeMillis();
        String s3Key = "reconciliation/" + legalEntityId + "/" + batchCode + "/" + fileName;

        ReconciliationBatch batch = ReconciliationBatch.builder()
                .batchCode(batchCode)
                .legalEntity(legalEntity)
                .sourceType(sourceType)
                .fileName(fileName)
                .s3Key(s3Key)
                .fileHash(fileHash)
                .status(BatchStatus.PARSING)
                .createdBy(performedBy)
                .build();

        // 2. Parse file CSV
        List<ParsedTransaction> parsedRows;
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            parsedRows = shopeeSettlementParser.parse(is);
        } catch (Exception e) {
            log.error("Failed to parse settlement file: {}", e.getMessage());
            batch.setStatus(BatchStatus.DISCREPANCY_FOUND);
            batch.setNotes("Lỗi định dạng file: " + e.getMessage());
            return batchRepository.save(batch);
        }

        batch.setStatus(BatchStatus.MATCHING);
        batch.setTotalRecords(parsedRows.size());

        BigDecimal sumExtAmount = BigDecimal.ZERO;
        BigDecimal sumIntAmount = BigDecimal.ZERO;
        int matchedCount = 0;
        int discrepancyCount = 0;

        // 3. Tiến hành so khớp (Matching Engine)
        for (ParsedTransaction ext : parsedRows) {
            sumExtAmount = sumExtAmount.add(ext.netAmount());

            Optional<FinanceTransaction> internalOpt = financeTransactionRepository
                    .findFirstByLegalEntityIdAndSourceId(legalEntityId, ext.orderId());

            if (internalOpt.isEmpty()) {
                // Sàn thanh toán nhưng nội bộ không có đơn
                ReconciliationItem item = ReconciliationItem.builder()
                        .externalOrderId(ext.orderId())
                        .internalOrderId(null)
                        .externalAmount(ext.netAmount())
                        .internalAmount(BigDecimal.ZERO)
                        .discrepancyAmount(ext.netAmount())
                        .feeExternal(ext.feeAmount())
                        .feeInternal(BigDecimal.ZERO)
                        .matchStatus(MatchStatus.UNMATCHED_EXTERNAL)
                        .resolutionStatus(ResolutionStatus.PENDING)
                        .resolutionNotes("Đơn hàng ngoài sàn không tìm thấy trong hệ thống nội bộ")
                        .build();
                batch.addItem(item);
                discrepancyCount++;
            } else {
                FinanceTransaction internalTx = internalOpt.get();
                BigDecimal internalAmount = internalTx.getAmount();
                sumIntAmount = sumIntAmount.add(internalAmount);

                // Tính tổng phí nội bộ từ lines (nếu có)
                BigDecimal internalFee = BigDecimal.ZERO;
                if (internalTx.getLines() != null) {
                    for (FinanceTransactionLine line : internalTx.getLines()) {
                        if ("PLATFORM_FEE".equalsIgnoreCase(line.getLineType()) || (line.getLineType() != null && line.getLineType().contains("FEE"))) {
                            internalFee = internalFee.add(line.getAmount().abs());
                        }
                    }
                }

                BigDecimal diff = ext.netAmount().subtract(internalAmount).abs();
                boolean isExact = diff.compareTo(new BigDecimal("0.0001")) <= 0;

                ReconciliationItem item = ReconciliationItem.builder()
                        .externalOrderId(ext.orderId())
                        .internalOrderId(internalTx.getTransactionCode())
                        .externalAmount(ext.netAmount())
                        .internalAmount(internalAmount)
                        .discrepancyAmount(ext.netAmount().subtract(internalAmount))
                        .feeExternal(ext.feeAmount())
                        .feeInternal(internalFee)
                        .matchStatus(isExact ? MatchStatus.EXACT_MATCH : MatchStatus.FEE_MISMATCH)
                        .resolutionStatus(isExact ? ResolutionStatus.RESOLVED : ResolutionStatus.PENDING)
                        .resolutionNotes(isExact ? "Khớp hoàn toàn 100%" : "Phát hiện chênh lệch phí sàn: " + diff + " VND")
                        .build();

                batch.addItem(item);
                if (isExact) {
                    matchedCount++;
                } else {
                    discrepancyCount++;
                }
            }
        }

        batch.setMatchedRecords(matchedCount);
        batch.setDiscrepancyRecords(discrepancyCount);
        batch.setTotalAmountExternal(sumExtAmount.setScale(4, RoundingMode.HALF_UP));
        batch.setTotalAmountInternal(sumIntAmount.setScale(4, RoundingMode.HALF_UP));
        batch.setStatus(discrepancyCount > 0 ? BatchStatus.DISCREPANCY_FOUND : BatchStatus.COMPLETED);

        log.info("Finished reconciliation batch {}: total={}, matched={}, discrepancy={}",
                batchCode, batch.getTotalRecords(), matchedCount, discrepancyCount);

        return batchRepository.save(batch);
    }
}
