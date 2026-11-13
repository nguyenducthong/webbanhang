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
public class SellerSnapshot implements Serializable {
    private String legalName;
    private String taxCode;
    private String address;
    private String phoneNumber;
    private String email;
    private String legalRepresentative;
    private String bankAccountNumber;
    private String bankName;
}
