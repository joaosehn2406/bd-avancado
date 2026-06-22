package com.jps.jps.shipment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String trackingCode) {
        super("Shipment not found: " + trackingCode);
    }
}
