package com.nexus.fulfillment.infrastructure.persistence;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import com.nexus.fulfillment.domain.model.ReservationStatus;
import com.nexus.fulfillment.infrastructure.persistence.entity.ReservationJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository jpaRepository;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        jpaRepository.save(toEntity(reservation));
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return jpaRepository.findById(uuid).map(this::toDomain);
    }

    @Override
    public Optional<Reservation> findByReferenceId(String referenceId) {
        return jpaRepository.findByReferenceId(referenceId).map(this::toDomain);
    }

    @Override
    public List<Reservation> findExpiredPending(Instant now) {
        return jpaRepository.findByStatusAndExpiresAtLessThanEqual(ReservationStatus.PENDING.name(), now).stream()
                .map(this::toDomain).toList();
    }

    private ReservationJpaEntity toEntity(Reservation r) {
        return new ReservationJpaEntity(UUID.fromString(r.getId()), r.getReferenceId(), r.getSkuId(),
                r.getWarehouseId(), r.getQuantity(), r.getStatus().name(), r.getExpiresAt(), r.getCreatedAt());
    }

    private Reservation toDomain(ReservationJpaEntity e) {
        return Reservation.reconstitute(e.getId().toString(), e.getReferenceId(), e.getSkuId(), e.getWarehouseId(),
                e.getQuantity(), ReservationStatus.valueOf(e.getStatus()), e.getExpiresAt(), e.getCreatedAt());
    }
}
