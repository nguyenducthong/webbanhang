package com.fashion.platform.finance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryReversalTest {

    @Test
    @DisplayName("Should successfully reverse a posted journal entry with swapped debit/credit lines")
    void testJournalEntryReversal() {
        AccountingPeriod period = AccountingPeriod.builder()
                .status(PeriodStatus.OPEN)
                .periodCode("2026-M11")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .build();

        AccountingAccount cash = AccountingAccount.builder().accountCode("111").accountName("Tiền mặt").build();
        AccountingAccount revenue = AccountingAccount.builder().accountCode("511").accountName("Doanh thu").build();

        JournalEntry original = JournalEntry.builder()
                .period(period)
                .entryNo("JE-ORIGINAL")
                .postingDate(LocalDate.of(2026, 11, 10))
                .entryStatus(JournalEntryStatus.DRAFT)
                .build();

        // Gốc: Nợ 111 (100k) / Có 511 (100k)
        original.addLine(JournalEntryLine.builder().account(cash).debitAmount(new BigDecimal("100000.0000")).creditAmount(BigDecimal.ZERO).build());
        original.addLine(JournalEntryLine.builder().account(revenue).debitAmount(BigDecimal.ZERO).creditAmount(new BigDecimal("100000.0000")).build());

        original.post(UUID.randomUUID());
        assertThat(original.getEntryStatus()).isEqualTo(JournalEntryStatus.POSTED);

        // Tạo bút toán đảo
        UUID actorId = UUID.randomUUID();
        JournalEntry reversal = original.createReversal("JE-REVERSAL", period, actorId);

        // Kiểm tra bút toán gốc
        assertThat(original.getEntryStatus()).isEqualTo(JournalEntryStatus.REVERSED);
        assertThat(original.getReversedBy()).isEqualTo(reversal);

        // Kiểm tra bút toán đảo: Nợ 511 (100k) / Có 111 (100k)
        assertThat(reversal.getEntryStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(reversal.getReverses()).isEqualTo(original);
        assertThat(reversal.getEntryNo()).isEqualTo("JE-REVERSAL");

        JournalEntryLine revLine1 = reversal.getLines().get(0);
        assertThat(revLine1.getAccount()).isEqualTo(cash);
        assertThat(revLine1.getDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(revLine1.getCreditAmount()).isEqualByComparingTo(new BigDecimal("100000.0000")); // Có 111

        JournalEntryLine revLine2 = reversal.getLines().get(1);
        assertThat(revLine2.getAccount()).isEqualTo(revenue);
        assertThat(revLine2.getDebitAmount()).isEqualByComparingTo(new BigDecimal("100000.0000")); // Nợ 511
        assertThat(revLine2.getCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should prevent double reversal of an already reversed journal entry")
    void testPreventDoubleReversal() {
        AccountingPeriod period = AccountingPeriod.builder().status(PeriodStatus.OPEN).periodCode("2026-M11").startDate(LocalDate.now()).endDate(LocalDate.now()).build();
        AccountingAccount cash = AccountingAccount.builder().accountCode("111").build();

        JournalEntry original = JournalEntry.builder().period(period).entryNo("JE-DUP").postingDate(LocalDate.now()).entryStatus(JournalEntryStatus.DRAFT).build();
        original.addLine(JournalEntryLine.builder().account(cash).debitAmount(BigDecimal.TEN).creditAmount(BigDecimal.ZERO).build());
        original.addLine(JournalEntryLine.builder().account(cash).debitAmount(BigDecimal.ZERO).creditAmount(BigDecimal.TEN).build());
        original.post(UUID.randomUUID());

        original.createReversal("JE-REV-1", period, UUID.randomUUID());

        // Lần thứ 2 đảo lại sẽ bị từ chối
        assertThatThrownBy(() -> original.createReversal("JE-REV-2", period, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been reversed");
    }
}
