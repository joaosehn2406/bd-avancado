package com.jps.jps.event.eventByCity;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventByCityService {

    private final EventByCityRepository repository;

    public EventByCityService(EventByCityRepository repository) {
        this.repository = repository;
    }

    public CityActivityResponse findByCity(String city, LocalDate date) {
        List<CityEventItem> items = repository.findByCityAndDateBucket(city, date)
                .stream()
                .map(CityEventItem::from)
                .toList();
        return new CityActivityResponse(city, date, items);
    }
}
