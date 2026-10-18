package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Takes the auctioned unit out of stock for good once the winner paid. Idempotent through
// CommitReservationUseCase.
public class CommitAuctionHoldUseCase {

    private static final Logger log = LoggerFactory.getLogger(CommitAuctionHoldUseCase.class);

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final CommitReservationUseCase commitReservationUseCase;

    public CommitAuctionHoldUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                    CommitReservationUseCase commitReservationUseCase) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.commitReservationUseCase = commitReservationUseCase;
    }

    public void commit(String auctionId) {
        reservationRepositoryPort.findByReferenceId(HoldForAuctionUseCase.referenceFor(auctionId))
                .ifPresentOrElse(r -> commitReservationUseCase.commit(r.getId()),
                        () -> log.warn("Auction {} was paid but had no held unit (created before holds "
                                + "existed?): stock not decremented", auctionId));
    }
}
