package com.jps.jps.event.eventByStatus;

import com.jps.jps.event.eventByCode.EventStatus;
import com.jps.jps.event.eventByCode.EventStatusResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventByStatusService {

    private final EventByStatusRepository repository;

    public EventByStatusService(EventByStatusRepository repository) {
        this.repository = repository;
    }

    public StatusActivityResponse findByStatus(Integer statusId, LocalDate date) {
        EventStatusResponse statusResponse = EventStatusResponse.from(EventStatus.fromId(statusId));
        List<StatusEventItem> items = repository.findByStatusAndDateBucket(statusId, date)
                .stream()
                .map(StatusEventItem::from)
                .toList();
        return new StatusActivityResponse(statusResponse, date, items);
    }
}
