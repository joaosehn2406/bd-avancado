package com.jps.jps.shipment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> create(@RequestBody @Valid ShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.create(request));
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<ShipmentResponse> findByTrackingCode(@PathVariable String trackingCode) {
        return ResponseEntity.ok(shipmentService.getByTrackingCode(trackingCode));
    }

    @DeleteMapping("/{trackingCode}")
    public ResponseEntity<Void> delete(@PathVariable String trackingCode) {
        shipmentService.delete(trackingCode);
        return ResponseEntity.noContent().build();
    }
}
