package com.jps.jps.event.eventByCity;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/hubs")
public class EventByCityController {

    private final EventByCityService service;

    public EventByCityController(EventByCityService service) {
        this.service = service;
    }

    @GetMapping("/{city}/hoje")
    public ResponseEntity<CityActivityResponse> getHubActivity(@PathVariable String city) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return ResponseEntity.ok(service.findByCity(city, today));
    }
}
