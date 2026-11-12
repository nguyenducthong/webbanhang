package com.fashion.platform.infra.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentConsumerService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean processIfNotExists(String consumerName, UUID eventId, Runnable processingLogic) {
        if (processedEventRepository.existsByConsumerNameAndEventId(consumerName, eventId)) {
            log.info("Duplicate event detected: consumer={}, eventId={}. Skipping.", consumerName, eventId);
            return false;
        }

        processingLogic.run();

        ProcessedEventEntity entity = ProcessedEventEntity.builder()
                .consumerName(consumerName)
                .eventId(eventId)
                .processedAt(Instant.now())
                .build();
        processedEventRepository.save(entity);
        return true;
    }
}

