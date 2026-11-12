package com.fashion.platform.finance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_code", nullable = false, unique = true, length = 50)
    private String entityCode;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 40)
    @Builder.Default
    private LegalEntityType entityType = LegalEntityType.UNDECIDED;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "business_registration_no", length = 100)
    private String businessRegistrationNo;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "country_code", nullable = false, length = 2)
    @Builder.Default
    private String countryCode = "VN";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private LegalEntityStatus status = LegalEntityStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private UUID updatedBy;

    public void activate(UUID actorId) {
        this.status = LegalEntityStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void deactivate(UUID actorId) {
        this.status = LegalEntityStatus.INACTIVE;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }

    public void updateDetails(String entityName, LegalEntityType entityType, String taxCode,
                              String businessRegistrationNo, String address, UUID actorId) {
        this.entityName = entityName;
        if (entityType != null) {
            this.entityType = entityType;
        }
        this.taxCode = taxCode;
        this.businessRegistrationNo = businessRegistrationNo;
        this.address = address;
        this.updatedAt = Instant.now();
        this.updatedBy = actorId;
    }
}
