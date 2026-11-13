package com.fashion.platform.reconciliation.domain.model;

public enum MatchStatus {
    EXACT_MATCH,          // Khớp 100% doanh thu và phí
    FEE_MISMATCH,         // Khớp mã đơn nhưng chênh lệch phí sàn / chiết khấu
    UNMATCHED_EXTERNAL,   // Sàn báo có đơn nhưng nội bộ chưa ghi nhận
    UNMATCHED_INTERNAL    // Nội bộ có đơn nhưng sàn chưa thanh toán
}
