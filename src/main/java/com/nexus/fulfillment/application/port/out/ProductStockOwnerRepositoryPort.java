package com.nexus.fulfillment.application.port.out;

import java.util.Optional;

public interface ProductStockOwnerRepositoryPort {

    // Records the owner of a product's stock only if none is recorded yet. Returns true when
    // this call created it, false when it already existed -- which is how a redelivered
    // ProductCreated event is recognised as a duplicate (Kafka is at-least-once).
    boolean registerIfAbsent(String productId, String sellerId);

    Optional<String> findSellerId(String productId);
}
