package com.fashion.platform.loader;

import com.fashion.platform.finance.domain.entity.LegalEntity;
import com.fashion.platform.finance.domain.entity.LegalEntityStatus;
import com.fashion.platform.finance.domain.entity.LegalEntityType;
import com.fashion.platform.finance.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final LegalEntityRepository legalEntityRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!legalEntityRepository.existsByEntityCode("LE-DEMO-FASHION")) {
            LegalEntity demo = LegalEntity.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .entityCode("LE-DEMO-FASHION")
                    .entityName("Fashion Demo Store VN")
                    .entityType(LegalEntityType.COMPANY)
                    .taxCode("0101234567")
                    .address("123 Le Loi, District 1, Ho Chi Minh City")
                    .countryCode("VN")
                    .status(LegalEntityStatus.ACTIVE)
                    .build();

            legalEntityRepository.save(demo);
            log.info("Initialized DEMO Legal Entity: LE-DEMO-FASHION");
        }
    }
}

