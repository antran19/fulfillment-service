package com.nexus.fulfillment.application.usecase;

import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.domain.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionHoldUseCasesTest {

    private final ReservationRepositoryPort reservationRepositoryPort = mock(ReservationRepositoryPort.class);
    private final ReleaseReservationUseCase releaseReservationUseCase = mock(ReleaseReservationUseCase.class);
    private final CommitReservationUseCase commitReservationUseCase = mock(CommitReservationUseCase.class);
    private final ReleaseAuctionHoldUseCase release = new ReleaseAuctionHoldUseCase(reservationRepositoryPort, releaseReservationUseCase);
    private final CommitAuctionHoldUseCase commit = new CommitAuctionHoldUseCase(reservationRepositoryPort, commitReservationUseCase);

    private final Reservation hold = Reservation.create("auction:auction-1", "product-1", "MAIN", 1);

    @Test
    void release_findsTheHoldByTheAuctionsReference() {
        when(reservationRepositoryPort.findByReferenceId("auction:auction-1")).thenReturn(Optional.of(hold));

        release.release("auction-1");

        verify(releaseReservationUseCase).release(hold.getId());
    }

    @Test
    void commit_findsTheHoldByTheAuctionsReference() {
        when(reservationRepositoryPort.findByReferenceId("auction:auction-1")).thenReturn(Optional.of(hold));

        commit.commit("auction-1");

        verify(commitReservationUseCase).commit(hold.getId());
    }

    @Test
    void anAuctionWithoutHold_isANoOp() {
        // Auctions created before holds existed, or whose hold already failed.
        when(reservationRepositoryPort.findByReferenceId(any())).thenReturn(Optional.empty());

        release.release("old-auction");
        commit.commit("old-auction");

        verifyNoInteractions(releaseReservationUseCase, commitReservationUseCase);
    }
}
