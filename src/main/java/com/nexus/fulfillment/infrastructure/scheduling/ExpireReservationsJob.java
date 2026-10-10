package com.nexus.fulfillment.infrastructure.scheduling;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.application.usecase.ReleaseReservationUseCase;
import com.nexus.fulfillment.domain.model.Reservation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

// SRS: "Reservations shall automatically expire and be released if not confirmed within the
// allowed time window." Reuses ReleaseReservationUseCase as-is -- an expiry and an explicit
// cancel both just mean "this hold no longer applies."
@Component
public class ExpireReservationsJob {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReleaseReservationUseCase releaseReservationUseCase;

    public ExpireReservationsJob(ReservationRepositoryPort reservationRepositoryPort,
                                  ReleaseReservationUseCase releaseReservationUseCase) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.releaseReservationUseCase = releaseReservationUseCase;
    }

    @Scheduled(fixedDelay = 30000)
    public void run() {
        Instant now = Instant.now();
        for (Reservation reservation : reservationRepositoryPort.findExpiredPending(now)) {
            releaseReservationUseCase.release(reservation.getId());
        }
    }
}
