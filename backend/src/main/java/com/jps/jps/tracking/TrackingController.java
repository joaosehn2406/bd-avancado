package com.jps.jps.tracking;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/rastreio")
public class TrackingController {

    private final TrackingService trackingService;
    private final SseService sseService;

    public TrackingController(TrackingService trackingService, SseService sseService) {
        this.trackingService = trackingService;
        this.sseService = sseService;
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<TrackingResponse> getTracking(@PathVariable String trackingCode) {
        return ResponseEntity.ok(trackingService.getByTrackingCode(trackingCode));
    }

    @GetMapping(value = "/{trackingCode}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String trackingCode) {
        return sseService.subscribe(trackingCode);
    }
}
