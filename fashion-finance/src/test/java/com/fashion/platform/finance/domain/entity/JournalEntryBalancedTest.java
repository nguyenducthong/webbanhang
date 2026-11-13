package com.fashion.platform.finance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryBalancedTest {

    @Test
    @DisplayName("Should successfully post a balanced journal entry")
    void testBalancedJournalEntryPost() {
        AccountingPeriod period = AccountingPeriod.builder()
                .status(PeriodStatus.OPEN)
                .periodCode("2026-M11")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .build();

        AccountingAccount cashAccount = AccountingAccount.builder()
                .accountCode("111")
                .accountName("Tiền mặt")
                .accountType(AccountType.ASSET)
                .build();

        AccountingAccount revenueAccount = AccountingAccount.builder()
                .accountCode("511")
                .accountName("Doanh thu bán hàng")
                .accountType(AccountType.REVENUE)
                .build();

        JournalEntry entry = JournalEntry.builder()
                .period(period)
                .entryNo("JE-2026-001")
                .postingDate(LocalDate.of(2026, 11, 12))
                .entryStatus(JournalEntryStatus.DRAFT)
                .build();

        // Nợ TK 111: 500,000 VND
        entry.addLine(JournalEntryLine.builder()
                .account(cashAccount)
                .debitAmount(new BigDecimal("500000.0000"))
                .creditAmount(BigDecimal.ZERO)
                .build());

        // Có TK 511: 500,000 VND
        entry.addLine(JournalEntryLine.builder()
                .account(revenueAccount)
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("500000.0000"))
                .build());

        assertThat(entry.getTotalDebit()).isEqualByComparingTo(new BigDecimal("500000.0000"));
        assertThat(entry.getTotalCredit()).isEqualByComparingTo(new BigDecimal("500000.0000"));

        UUID actorId = UUID.randomUUID();
        entry.post(actorId);

        assertThat(entry.getEntryStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getPostedBy()).isEqualTo(actorId);
        assertThat(entry.getPostedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw UnbalancedJournalEntryException when debits do not equal credits")
    void testUnbalancedJournalEntryThrowsException() {
        AccountingPeriod period = AccountingPeriod.builder()
                .status(PeriodStatus.OPEN)
                .periodCode("2026-M11")
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .build();

        AccountingAccount acc1 = AccountingAccount.builder().accountCode("111").accountType(AccountType.ASSET).build();
        AccountingAccount acc2 = AccountingAccount.builder().accountCode("511").accountType(AccountType.REVENUE).build();

        JournalEntry entry = JournalEntry.builder()
                .period(period)
                .entryNo("JE-UNBALANCED")
                .postingDate(LocalDate.now())
                .entryStatus(JournalEntryStatus.DRAFT)
                .build();

        // Nợ: 500,000 nhưng Có: 450,000
        entry.addLine(JournalEntryLine.builder().account(acc1).debitAmount(new BigDecimal("500000.0000")).build());
        entry.addLine(JournalEntryLine.builder().account(acc2).creditAmount(new BigDecimal("450000.0000")).build());

        assertThatThrownBy(() -> entry.post(UUID.randomUUID()))
                .isInstanceOf(UnbalancedJournalEntryException.class)
                .hasMessageContaining("does not equal Total Credit");
    }

    @Test
    @DisplayName("Should block posting when accounting period is CLOSED")
    void testBlockedPostingOnClosedPeriod() {
        AccountingPeriod closedPeriod = AccountingPeriod.builder()
                .status(PeriodStatus.CLOSED)
                .periodCode("2026-M10")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 31))
                .build();

        AccountingAccount acc = AccountingAccount.builder().accountCode("111").build();

        JournalEntry entry = JournalEntry.builder()
                .period(closedPeriod)
                .entryNo("JE-CLOSED")
                .postingDate(LocalDate.now())
                .build();

        entry.addLine(JournalEntryLine.builder().account(acc).debitAmount(new BigDecimal("100000.0000")).build());
        entry.addLine(JournalEntryLine.builder().account(acc).creditAmount(new BigDecimal("100000.0000")).build());

        assertThatThrownBy(() -> entry.post(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot post journal entry into CLOSED accounting period");
    }
}
