package com.fashion.platform.infra.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID publish(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEventEntity entity = OutboxEventEntity.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .occurredAt(Instant.now())
                    .attempts(0)
                    .build();

            OutboxEventEntity saved = outboxEventRepository.save(entity);
            log.debug("Outbox event recorded: id={}, type={}, aggregate={}/{}", 
                    saved.getId(), eventType, aggregateType, aggregateId);
            return saved.getId();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize event payload to JSON", e);
        }
    }
}

