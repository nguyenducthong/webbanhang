package com.fashion.platform.finance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxConfigurationTest {

    @Test
    @DisplayName("Should correctly evaluate effective dates only when approved")
    void testEffectiveDateLogic() {
        TaxConfiguration tc = TaxConfiguration.builder()
                .taxType(TaxType.VAT)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .effectiveTo(LocalDate.of(2026, 12, 31))
                .build();

        // Not yet approved -> not effective
        assertThat(tc.isEffectiveAt(LocalDate.of(2026, 6, 1))).isFalse();

        // Approve it by another user
        tc.approve(UUID.randomUUID());
        assertThat(tc.isApproved()).isTrue();

        // Check date boundaries
        assertThat(tc.isEffectiveAt(LocalDate.of(2025, 12, 31))).isFalse();
        assertThat(tc.isEffectiveAt(LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(tc.isEffectiveAt(LocalDate.of(2026, 6, 15))).isTrue();
        assertThat(tc.isEffectiveAt(LocalDate.of(2026, 12, 31))).isTrue();
        assertThat(tc.isEffectiveAt(LocalDate.of(2027, 1, 1))).isFalse();
    }

    @Test
    @DisplayName("Should enforce Maker-Checker: creator cannot approve their own tax configuration")
    void testMakerCheckerViolation() {
        UUID creatorId = UUID.randomUUID();

        TaxConfiguration tc = TaxConfiguration.builder()
                .taxType(TaxType.VAT)
                .createdBy(creatorId)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        assertThatThrownBy(() -> tc.approve(creatorId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Maker-Checker violation");
    }

    @Test
    @DisplayName("Should succeed approval when approver is different from creator")
    void testMakerCheckerSuccess() {
        UUID creatorId = UUID.randomUUID();
        UUID approverId = UUID.randomUUID();

        TaxConfiguration tc = TaxConfiguration.builder()
                .taxType(TaxType.VAT)
                .createdBy(creatorId)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        tc.approve(approverId);
        assertThat(tc.isApproved()).isTrue();
        assertThat(tc.getApprovedBy()).isEqualTo(approverId);
        assertThat(tc.getApprovedAt()).isNotNull();
    }
}

