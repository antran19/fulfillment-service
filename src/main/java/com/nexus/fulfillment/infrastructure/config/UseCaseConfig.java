package com.nexus.fulfillment.infrastructure.config;

import com.nexus.fulfillment.application.port.out.EventPublisherPort;
import com.nexus.fulfillment.application.port.out.InventoryLedgerRepositoryPort;
import com.nexus.fulfillment.application.port.out.InventoryRecordRepositoryPort;
import com.nexus.fulfillment.application.port.out.ProductStockOwnerRepositoryPort;
import com.nexus.fulfillment.application.port.out.ReservationRepositoryPort;
import com.nexus.fulfillment.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public IntakeInventoryUseCase intakeInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                                            InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort,
                                                            InventoryLevelNotifier inventoryLevelNotifier) {
        return new IntakeInventoryUseCase(inventoryRecordRepositoryPort, inventoryLedgerRepositoryPort,
                inventoryLevelNotifier);
    }

    @Bean
    public InventoryLevelNotifier inventoryLevelNotifier(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                                         EventPublisherPort eventPublisherPort) {
        return new InventoryLevelNotifier(inventoryRecordRepositoryPort, eventPublisherPort);
    }

    @Bean
    public ReserveInventoryUseCase reserveInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                                              ReservationRepositoryPort reservationRepositoryPort,
                                                              InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort,
                                                              InventoryLevelNotifier inventoryLevelNotifier) {
        return new ReserveInventoryUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort, inventoryLevelNotifier);
    }

    @Bean
    public CommitReservationUseCase commitReservationUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                                                ReservationRepositoryPort reservationRepositoryPort,
                                                                InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort,
                                                                InventoryLevelNotifier inventoryLevelNotifier) {
        return new CommitReservationUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort, inventoryLevelNotifier);
    }

    @Bean
    public ReleaseReservationUseCase releaseReservationUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort,
                                                                  ReservationRepositoryPort reservationRepositoryPort,
                                                                  InventoryLedgerRepositoryPort inventoryLedgerRepositoryPort,
                                                                  InventoryLevelNotifier inventoryLevelNotifier) {
        return new ReleaseReservationUseCase(inventoryRecordRepositoryPort, reservationRepositoryPort,
                inventoryLedgerRepositoryPort, inventoryLevelNotifier);
    }

    @Bean
    public GetInventoryUseCase getInventoryUseCase(InventoryRecordRepositoryPort inventoryRecordRepositoryPort) {
        return new GetInventoryUseCase(inventoryRecordRepositoryPort);
    }

    @Bean
    public GetReservationUseCase getReservationUseCase(ReservationRepositoryPort reservationRepositoryPort) {
        return new GetReservationUseCase(reservationRepositoryPort);
    }

    @Bean
    public ReserveForOrderUseCase reserveForOrderUseCase(ReserveInventoryUseCase reserveInventoryUseCase,
                                                           EventPublisherPort eventPublisherPort) {
        return new ReserveForOrderUseCase(reserveInventoryUseCase, eventPublisherPort);
    }

    @Bean
    public FailOrderReservationUseCase failOrderReservationUseCase(EventPublisherPort eventPublisherPort) {
        return new FailOrderReservationUseCase(eventPublisherPort);
    }

    @Bean
    public CommitReservationsForOrderUseCase commitReservationsForOrderUseCase(
            ReservationRepositoryPort reservationRepositoryPort, CommitReservationUseCase commitReservationUseCase) {
        return new CommitReservationsForOrderUseCase(reservationRepositoryPort, commitReservationUseCase);
    }

    @Bean
    public ReleaseReservationsForOrderUseCase releaseReservationsForOrderUseCase(
            ReservationRepositoryPort reservationRepositoryPort, ReleaseReservationUseCase releaseReservationUseCase) {
        return new ReleaseReservationsForOrderUseCase(reservationRepositoryPort, releaseReservationUseCase);
    }

    @Bean
    public HoldForAuctionUseCase holdForAuctionUseCase(ReserveInventoryUseCase reserveInventoryUseCase,
                                                       EventPublisherPort eventPublisherPort) {
        return new HoldForAuctionUseCase(reserveInventoryUseCase, eventPublisherPort);
    }

    @Bean
    public ReleaseAuctionHoldUseCase releaseAuctionHoldUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                                               ReleaseReservationUseCase releaseReservationUseCase) {
        return new ReleaseAuctionHoldUseCase(reservationRepositoryPort, releaseReservationUseCase);
    }

    @Bean
    public CommitAuctionHoldUseCase commitAuctionHoldUseCase(ReservationRepositoryPort reservationRepositoryPort,
                                                             CommitReservationUseCase commitReservationUseCase) {
        return new CommitAuctionHoldUseCase(reservationRepositoryPort, commitReservationUseCase);
    }

    @Bean
    public RegisterProductStockUseCase registerProductStockUseCase(
            ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort,
            IntakeInventoryUseCase intakeInventoryUseCase) {
        return new RegisterProductStockUseCase(productStockOwnerRepositoryPort, intakeInventoryUseCase);
    }

    @Bean
    public RestockInventoryUseCase restockInventoryUseCase(
            ProductStockOwnerRepositoryPort productStockOwnerRepositoryPort,
            IntakeInventoryUseCase intakeInventoryUseCase) {
        return new RestockInventoryUseCase(productStockOwnerRepositoryPort, intakeInventoryUseCase);
    }

    @Bean
    public IntakeForReturnUseCase intakeForReturnUseCase(IntakeInventoryUseCase intakeInventoryUseCase) {
        return new IntakeForReturnUseCase(intakeInventoryUseCase);
    }
}
