package com.fashion.platform.reconciliation.application.service;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.reconciliation.api.dto.pos.*;
import com.fashion.platform.reconciliation.domain.model.PosShift;
import com.fashion.platform.reconciliation.domain.model.PosShiftCashCount;
import com.fashion.platform.reconciliation.domain.model.PosShiftStatus;
import com.fashion.platform.reconciliation.domain.repository.PosShiftCashCountRepository;
import com.fashion.platform.reconciliation.domain.repository.PosShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosShiftService {

    private final PosShiftRepository posShiftRepository;
    private final PosShiftCashCountRepository cashCountRepository;
    private final LegalEntityRepository legalEntityRepository;

    @Transactional
    public PosShiftResponse openShift(OpenShiftRequest request) {
        LegalEntity legalEntity = legalEntityRepository.findById(request.getLegalEntityId())
                .orElseThrow(() -> new IllegalArgumentException("Legal entity not found: " + request.getLegalEntityId()));

        posShiftRepository.findFirstByLegalEntityIdAndStoreCodeAndStatus(request.getLegalEntityId(), request.getStoreCode(), PosShiftStatus.OPEN)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Cửa hàng " + request.getStoreCode() + " hiện đang có ca bán hàng chưa đóng: " + existing.getShiftCode());
                });

        String shiftCode = request.getShiftCode();
        if (shiftCode == null || shiftCode.isBlank()) {
            shiftCode = "SHIFT-" + request.getStoreCode() + "-" + System.currentTimeMillis();
        }

        BigDecimal floatAmount = request.getOpeningFloat() != null ? request.getOpeningFloat() : BigDecimal.ZERO;

        PosShift shift = PosShift.builder()
                .legalEntity(legalEntity)
                .storeCode(request.getStoreCode())
                .shiftCode(shiftCode)
                .cashierUsername(request.getCashierUsername())
                .openedAt(Instant.now())
                .status(PosShiftStatus.OPEN)
                .openingFloat(floatAmount)
                .expectedCashAmount(floatAmount)
                .build();

        shift = posShiftRepository.save(shift);
        log.info("Opened new POS Shift {} for store {} by cashier {}", shift.getShiftCode(), shift.getStoreCode(), shift.getCashierUsername());
        return PosShiftResponse.fromEntity(shift);
    }

    @Transactional
    public PosShiftDetailResponse closeShift(UUID shiftId, CloseShiftRequest request) {
        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("POS Shift not found: " + shiftId));

        if (shift.getStatus() != PosShiftStatus.OPEN) {
            throw new IllegalStateException("Ca bán hàng này không ở trạng thái OPEN để đóng: " + shift.getStatus());
        }

        shift.setClosedAt(Instant.now());
        shift.setTotalCashSales(request.getTotalCashSales() != null ? request.getTotalCashSales() : BigDecimal.ZERO);
        shift.setTotalCardSales(request.getTotalCardSales() != null ? request.getTotalCardSales() : BigDecimal.ZERO);
        shift.setTotalQrSales(request.getTotalQrSales() != null ? request.getTotalQrSales() : BigDecimal.ZERO);

        BigDecimal expectedCash = shift.getOpeningFloat().add(shift.getTotalCashSales()).setScale(4, RoundingMode.HALF_UP);
        shift.setExpectedCashAmount(expectedCash);

        // Lưu bảng kê tiền mặt đếm thực tế
        BigDecimal totalCounted = BigDecimal.ZERO;
        if (request.getCashCounts() != null) {
            for (CashDenominationItem item : request.getCashCounts()) {
                BigDecimal denomVal = BigDecimal.valueOf(item.getDenomination());
                BigDecimal lineTotal = denomVal.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(4, RoundingMode.HALF_UP);
                totalCounted = totalCounted.add(lineTotal);

                PosShiftCashCount count = PosShiftCashCount.builder()
                        .shift(shift)
                        .denomination(item.getDenomination())
                        .quantity(item.getQuantity())
                        .totalAmount(lineTotal)
                        .build();
                cashCountRepository.save(count);
            }
        }

        totalCounted = totalCounted.setScale(4, RoundingMode.HALF_UP);
        shift.setActualCountedCash(totalCounted);
        BigDecimal diff = totalCounted.subtract(expectedCash).setScale(4, RoundingMode.HALF_UP);
        shift.setDifferenceAmount(diff);
        shift.setStatus(PosShiftStatus.CLOSED);
        shift.setClosingNotes(request.getClosingNotes());
        shift.setManagerApprovedBy(request.getPerformedBy());

        shift = posShiftRepository.save(shift);

        if (diff.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Shift {} closed BALANCED 100%. Total cash counted: {} VND", shift.getShiftCode(), totalCounted);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Shift {} closed with CASH SHORTAGE: {} VND", shift.getShiftCode(), diff);
        } else {
            log.info("Shift {} closed with CASH OVERAGE: {} VND", shift.getShiftCode(), diff);
        }

        List<PosShiftCashCount> savedCounts = cashCountRepository.findByShiftIdOrderByDenominationDesc(shiftId);
        return PosShiftDetailResponse.of(shift, savedCounts);
    }

    @Transactional(readOnly = true)
    public List<PosShiftResponse> getShiftsByLegalEntity(UUID legalEntityId) {
        return posShiftRepository.findByLegalEntityIdOrderByOpenedAtDesc(legalEntityId)
                .stream()
                .map(PosShiftResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PosShiftDetailResponse getShiftDetail(UUID shiftId) {
        PosShift shift = posShiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("POS Shift not found: " + shiftId));
        List<PosShiftCashCount> counts = cashCountRepository.findByShiftIdOrderByDenominationDesc(shiftId);
        return PosShiftDetailResponse.of(shift, counts);
    }
}
