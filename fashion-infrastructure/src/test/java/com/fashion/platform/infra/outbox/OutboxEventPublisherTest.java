package com.fashion.platform.infra.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("Should persist outbox event with serialized JSON payload and correct metadata")
    void testPublishEvent() {
        UUID expectedId = UUID.randomUUID();
        when(outboxEventRepository.save(any(OutboxEventEntity.class)))
                .thenAnswer(inv -> {
                    OutboxEventEntity entity = inv.getArgument(0);
                    entity.setId(expectedId);
                    return entity;
                });

        UUID returnedId = publisher.publish(
                "ORDER",
                "ORD-12345",
                "ORDER_CREATED",
                Map.of("amount", 250000, "currency", "VND")
        );

        assertThat(returnedId).isEqualTo(expectedId);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEventEntity captured = captor.getValue();
        assertThat(captured.getAggregateType()).isEqualTo("ORDER");
        assertThat(captured.getAggregateId()).isEqualTo("ORD-12345");
        assertThat(captured.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(captured.getPayload()).contains("250000");
        assertThat(captured.getPublishedAt()).isNull();
    }
}
