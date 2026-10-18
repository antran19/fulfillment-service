package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;

// Gives the auctioned unit back to stock. A no-op for an auction that never got a hold (created
// before holds existed, or whose hold failed) and, through ReleaseReservationUseCase, for a hold
// that was already released or committed -- several events may ask for the same release.
public class ReleaseAuctionHoldUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ReleaseReservationUseCase releaseReservationUseCase;

    public ReleaseAuctionHoldUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                     ReleaseReservationUseCase releaseReservationUseCase) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.releaseReservationUseCase = releaseReservationUseCase;
    }

    public void release(String auctionId) {
        reservationRepositoryPort.findByReferenceId(HoldForAuctionUseCase.referenceFor(auctionId))
                .ifPresent(r -> releaseReservationUseCase.release(r.getId()));
    }
}
