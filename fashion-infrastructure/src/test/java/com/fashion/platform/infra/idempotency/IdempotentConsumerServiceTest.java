package com.fashion.platform.infra.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotentConsumerServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private IdempotentConsumerService idempotentConsumerService;

    @Test
    @DisplayName("Should execute consumer logic when event is received for the first time")
    void testFirstTimeEventExecution() {
        String consumerName = "InvoiceEventListener";
        UUID eventId = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger(0);

        when(processedEventRepository.existsByConsumerNameAndEventId(consumerName, eventId))
                .thenReturn(false);

        boolean processed = idempotentConsumerService.processIfNotExists(consumerName, eventId, counter::incrementAndGet);

        assertThat(processed).isTrue();
        assertThat(counter.get()).isEqualTo(1);
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    @DisplayName("Should skip execution when event has already been processed")
    void testDuplicateEventSkipped() {
        String consumerName = "InvoiceEventListener";
        UUID eventId = UUID.randomUUID();
        AtomicInteger counter = new AtomicInteger(0);

        when(processedEventRepository.existsByConsumerNameAndEventId(consumerName, eventId))
                .thenReturn(true);

        boolean processed = idempotentConsumerService.processIfNotExists(consumerName, eventId, counter::incrementAndGet);

        assertThat(processed).isFalse();
        assertThat(counter.get()).isEqualTo(0);
        verify(processedEventRepository, never()).save(any(ProcessedEventEntity.class));
    }
}
