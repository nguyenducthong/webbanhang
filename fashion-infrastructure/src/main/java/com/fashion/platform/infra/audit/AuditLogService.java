package com.fashion.platform.infra.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SystemAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(UUID userId, UUID legalEntityId, UUID storeId,
                       String action, String resourceType, String resourceId,
                       Object detail, String ipAddress) {
        String detailJson = "{}";
        if (detail != null) {
            try {
                detailJson = objectMapper.writeValueAsString(detail);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize audit detail object: {}", e.getMessage());
            }
        }

        SystemAuditLogEntity entity = SystemAuditLogEntity.builder()
                .userId(userId)
                .legalEntityId(legalEntityId)
                .storeId(storeId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .detailJson(detailJson)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();

        repository.save(entity);
        log.debug("Recorded audit log for action: {} on {}/{}", action, resourceType, resourceId);
    }
}

