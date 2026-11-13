package com.fashion.platform.reconciliation.domain.model;

public enum ResolutionStatus {
    PENDING,    // Chờ xử lý
    RESOLVED,   // Đã giải quyết / Đã tạo bút toán điều chỉnh
    IGNORED     // Bỏ qua chênh lệch trong hạn mức cho phép
}
