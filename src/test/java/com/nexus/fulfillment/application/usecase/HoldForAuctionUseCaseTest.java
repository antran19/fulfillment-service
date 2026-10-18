package com.nexus.fulfillment.application.usecase;

import com.nexus.common.events.AuctionHoldFailedEvent;
import com.nexus.fulfillment.application.exception.InsufficientInventoryException;
import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HoldForAuctionUseCaseTest {

    private final ReserveInventoryUseCase reserveInventoryUseCase = mock(ReserveInventoryUseCase.class);
    private final EventPublisherPort eventPublisherPort = mock(EventPublisherPort.class);
    private final HoldForAuctionUseCase useCase = new HoldForAuctionUseCase(reserveInventoryUseCase, eventPublisherPort);

    @Test
    void hold_reservesOneUnitUnderTheAuctionsReference_forTenDays() {
        useCase.hold("auction-1", "product-1");

        verify(reserveInventoryUseCase).reserve("auction:auction-1", "product-1", "MAIN", 1, Duration.ofDays(10));
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    void hold_withoutStock_tellsAuctionServiceInsteadOfFailing() {
        doThrow(new InsufficientInventoryException("product-1", "MAIN", 1))
                .when(reserveInventoryUseCase).reserve(eq("auction:auction-1"), eq("product-1"), eq("MAIN"), eq(1), any());

        assertThatCode(() -> useCase.hold("auction-1", "product-1")).doesNotThrowAnyException();

        verify(eventPublisherPort).publish(argThat(e -> e instanceof AuctionHoldFailedEvent evt
                && evt.getAuctionId().equals("auction-1")));
    }
}
