package com.jps.jps.event.eventByStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/status")
public class EventByStatusController {

    private final EventByStatusService service;

    public EventByStatusController(EventByStatusService service) {
        this.service = service;
    }

    @GetMapping("/{statusId}/hoje")
    public ResponseEntity<StatusActivityResponse> getStatusActivity(@PathVariable Integer statusId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return ResponseEntity.ok(service.findByStatus(statusId, today));
    }
}
