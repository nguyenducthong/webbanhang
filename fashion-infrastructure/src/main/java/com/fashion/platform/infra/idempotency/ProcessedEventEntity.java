package com.fashion.platform.infra.idempotency;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProcessedEventEntity.ProcessedEventId.class)
public class ProcessedEventEntity {

    @Id
    @Column(name = "consumer_name", nullable = false, length = 150)
    private String consumerName;

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessedEventId implements Serializable {
        private String consumerName;
        private UUID eventId;
    }
}

