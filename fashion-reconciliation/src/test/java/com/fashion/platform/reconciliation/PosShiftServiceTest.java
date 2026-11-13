package com.fashion.platform.reconciliation;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import com.fashion.platform.reconciliation.api.dto.pos.*;
import com.fashion.platform.reconciliation.application.service.PosShiftService;
import com.fashion.platform.reconciliation.domain.model.PosShift;
import com.fashion.platform.reconciliation.domain.model.PosShiftStatus;
import com.fashion.platform.reconciliation.domain.repository.PosShiftCashCountRepository;
import com.fashion.platform.reconciliation.domain.repository.PosShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosShiftServiceTest {

    @Mock
    private PosShiftRepository posShiftRepository;

    @Mock
    private PosShiftCashCountRepository cashCountRepository;

    @Mock
    private LegalEntityRepository legalEntityRepository;

    @InjectMocks
    private PosShiftService posShiftService;

    private UUID entityId;
    private LegalEntity legalEntity;

    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
        legalEntity = LegalEntity.builder()
                .id(entityId)
                .entityName("Fashion Store VN")
                .status(LegalEntityStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should open new POS Shift successfully")
    void shouldOpenShiftSuccessfully() {
        OpenShiftRequest request = OpenShiftRequest.builder()
                .legalEntityId(entityId)
                .storeCode("STORE-01")
                .cashierUsername("thu_ngan_a")
                .openingFloat(new BigDecimal("2000000.0000"))
                .build();

        when(legalEntityRepository.findById(entityId)).thenReturn(Optional.of(legalEntity));
        when(posShiftRepository.findFirstByLegalEntityIdAndStoreCodeAndStatus(entityId, "STORE-01", PosShiftStatus.OPEN))
                .thenReturn(Optional.empty());
        when(posShiftRepository.save(any(PosShift.class))).thenAnswer(i -> {
            PosShift s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        PosShiftResponse response = posShiftService.openShift(request);

        assertNotNull(response);
        assertEquals("STORE-01", response.getStoreCode());
        assertEquals("thu_ngan_a", response.getCashierUsername());
        assertEquals(PosShiftStatus.OPEN, response.getStatus());
        assertEquals(new BigDecimal("2000000.0000"), response.getOpeningFloat());
    }

    @Test
    @DisplayName("Should close POS Shift with balanced cash count 100%")
    void shouldCloseShiftBalanced() {
        UUID shiftId = UUID.randomUUID();
        PosShift shift = PosShift.builder()
                .id(shiftId)
                .legalEntity(legalEntity)
                .storeCode("STORE-01")
                .shiftCode("SHIFT-01-TEST")
                .cashierUsername("thu_ngan_a")
                .openedAt(Instant.now().minusSeconds(28800))
                .openingFloat(new BigDecimal("2000000.0000"))
                .expectedCashAmount(new BigDecimal("2000000.0000"))
                .status(PosShiftStatus.OPEN)
                .build();

        when(posShiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(posShiftRepository.save(any(PosShift.class))).thenAnswer(i -> i.getArgument(0));

        // Bán tiền mặt 3,500,000 -> Kỳ vọng két có: 2,000,000 + 3,500,000 = 5,500,000 VND
        // Kiểm đếm thực tế: 10 tờ 500k = 5,000,000 + 5 tờ 100k = 500,000 -> Tổng đếm = 5,500,000 VND
        CloseShiftRequest closeReq = CloseShiftRequest.builder()
                .totalCashSales(new BigDecimal("3500000.0000"))
                .totalCardSales(new BigDecimal("1200000.0000"))
                .totalQrSales(new BigDecimal("800000.0000"))
                .cashCounts(List.of(
                        new CashDenominationItem(500000, 10),
                        new CashDenominationItem(100000, 5)
                ))
                .closingNotes("Chốt ca thành công khớp tiền 100%")
                .performedBy("quan_ly_cua_hang")
                .build();

        PosShiftDetailResponse detail = posShiftService.closeShift(shiftId, closeReq);

        assertNotNull(detail);
        assertEquals(PosShiftStatus.CLOSED, detail.getShift().getStatus());
        assertEquals(0, new BigDecimal("5500000").compareTo(detail.getShift().getExpectedCashAmount()));
        assertEquals(0, new BigDecimal("5500000").compareTo(detail.getShift().getActualCountedCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(detail.getShift().getDifferenceAmount()));
    }

    @Test
    @DisplayName("Should detect cash shortage when counted cash is less than expected")
    void shouldDetectCashShortage() {
        UUID shiftId = UUID.randomUUID();
        PosShift shift = PosShift.builder()
                .id(shiftId)
                .legalEntity(legalEntity)
                .storeCode("STORE-01")
                .shiftCode("SHIFT-01-SHORTAGE")
                .openingFloat(new BigDecimal("1000000.0000"))
                .expectedCashAmount(new BigDecimal("1000000.0000"))
                .status(PosShiftStatus.OPEN)
                .build();

        when(posShiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(posShiftRepository.save(any(PosShift.class))).thenAnswer(i -> i.getArgument(0));

        // Bán tiền mặt 2,000,000 -> Kỳ vọng két có 3,000,000 VND
        // Kiểm đếm thực tế: chỉ có 5 tờ 500k = 2,500,000 VND -> Thiếu 500,000 VND
        CloseShiftRequest closeReq = CloseShiftRequest.builder()
                .totalCashSales(new BigDecimal("2000000.0000"))
                .cashCounts(List.of(
                        new CashDenominationItem(500000, 5)
                ))
                .closingNotes("Nghi vấn thối nhầm tiền cho khách đơn 002")
                .performedBy("thu_ngan_b")
                .build();

        PosShiftDetailResponse detail = posShiftService.closeShift(shiftId, closeReq);

        assertNotNull(detail);
        assertEquals(0, new BigDecimal("3000000").compareTo(detail.getShift().getExpectedCashAmount()));
        assertEquals(0, new BigDecimal("2500000").compareTo(detail.getShift().getActualCountedCash()));
        assertEquals(0, new BigDecimal("-500000").compareTo(detail.getShift().getDifferenceAmount()));
    }
}
