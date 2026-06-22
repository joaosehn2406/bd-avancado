package com.jps.jps.shipment;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public Shipment findByTrackingCode(String trackingCode) {
        return shipmentRepository.findById(trackingCode)
                .orElseThrow(() -> new ShipmentNotFoundException(trackingCode));
    }

    public ShipmentResponse getByTrackingCode(String trackingCode) {
        return ShipmentResponse.from(findByTrackingCode(trackingCode));
    }

    public ShipmentResponse create(ShipmentRequest request) {
        Shipment shipment = new Shipment(
                generateTrackingCode(),
                request.sender(),
                request.recipient(),
                request.origin(),
                request.destination(),
                Instant.now(),
                request.weightKg()
        );

        shipmentRepository.save(shipment);
        return ShipmentResponse.from(shipment);
    }

    public void delete(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be blank");
        }
        shipmentRepository.deleteById(trackingCode);
    }

    private String generateTrackingCode() {
        return "BR" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }
}
