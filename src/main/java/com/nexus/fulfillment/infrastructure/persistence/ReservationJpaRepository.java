package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {
    Optional<ReservationJpaEntity> findByReferenceId(String referenceId);
    List<ReservationJpaEntity> findByStatusAndExpiresAtLessThanEqual(String status, Instant now);
}
