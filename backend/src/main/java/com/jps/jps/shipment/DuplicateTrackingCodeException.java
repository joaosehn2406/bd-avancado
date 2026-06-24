package com.jps.jps.shipment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateTrackingCodeException extends RuntimeException {
    public DuplicateTrackingCodeException(String trackingCode) {
        super("Tracking code already exists: " + trackingCode);
    }
}
