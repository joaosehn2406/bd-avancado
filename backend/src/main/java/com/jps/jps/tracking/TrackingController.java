package com.jps.jps.tracking;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rastreio")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable String trackingCode) {
        return ResponseEntity.ok(trackingService.getByTrackingCode(trackingCode));
    }
}
