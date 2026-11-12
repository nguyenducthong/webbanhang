package com.fashion.platform.infra.outbox;

import com.fashion.platform.infra.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${fashion.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relayEvents() {
        List<OutboxEventEntity> pendingEvents = outboxEventRepository.findUnpublishedEvents(PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pendingEvents) {
            try {
                // routing key pattern: aggregateType.eventType, e.g. order.created, invoice.issued
                String routingKey = event.getAggregateType().toLowerCase() + "." + event.getEventType().toLowerCase();
                rabbitTemplate.convertAndSend(RabbitMqConfig.FASHION_EVENTS_EXCHANGE, routingKey, event.getPayload());

                event.setPublishedAt(Instant.now());
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(null);
            } catch (Exception e) {
                log.error("Failed to relay outbox event id={}: {}", event.getId(), e.getMessage());
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
            }
        }
        outboxEventRepository.saveAll(pendingEvents);
    }
}

