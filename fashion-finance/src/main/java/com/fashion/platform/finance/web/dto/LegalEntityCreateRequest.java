package com.fashion.platform.finance.web.dto;

import com.fashion.platform.finance.domain.entity.LegalEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalEntityCreateRequest {

    @NotBlank(message = "Entity code is required")
    @Size(max = 50, message = "Entity code cannot exceed 50 characters")
    private String entityCode;

    @NotBlank(message = "Entity name is required")
    @Size(max = 255, message = "Entity name cannot exceed 255 characters")
    private String entityName;

    private LegalEntityType entityType;
    private String taxCode;
    private String businessRegistrationNo;
    private String address;
    private String countryCode;
}

