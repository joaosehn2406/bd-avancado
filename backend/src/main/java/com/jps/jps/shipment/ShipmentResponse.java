package com.jps.jps.shipment;

import java.math.BigDecimal;
import java.time.Instant;

public record ShipmentResponse(
        String trackingCode,
        String sender,
        String recipient,
        String origin,
        String destination,
        Instant createdAt,
        BigDecimal weightKg
) {
    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.trackingCode(),
                shipment.sender(),
                shipment.recipient(),
                shipment.origin(),
                shipment.destination(),
                shipment.createdAt(),
                shipment.weightKg()
        );
    }
}
