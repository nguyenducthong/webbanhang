package com.fashion.platform.einvoice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerSnapshot implements Serializable {
    private String buyerName;              // Tên người mua hàng (cá nhân hoặc đại diện)
    private String legalName;              // Tên đơn vị / công ty (nếu xuất cho doanh nghiệp)
    private String taxCode;                // Mã số thuế doanh nghiệp (nếu có)
    private String address;                // Địa chỉ người mua
    private String phoneNumber;            // Số điện thoại
    private String email;                  // Email nhận hóa đơn
    private boolean enterprise;            // true nếu là khách hàng doanh nghiệp yêu cầu xuất VAT
}
