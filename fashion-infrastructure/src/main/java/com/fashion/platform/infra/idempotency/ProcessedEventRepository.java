package com.fashion.platform.infra.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.ProcessedEventId> {
    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);
}

