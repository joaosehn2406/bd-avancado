package com.jps.jps.event.eventByCode;

import com.jps.jps.event.eventByCity.EventByCity;
import com.jps.jps.event.eventByStatus.EventByStatus;
import com.jps.jps.tracking.SseService;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class EventByCodeService {

    private final EventByCodeRepository eventByCodeRepository;
    private final CassandraTemplate cassandraTemplate;
    private final SseService sseService;

    public EventByCodeService(EventByCodeRepository eventByCodeRepository,
                              CassandraTemplate cassandraTemplate,
                              SseService sseService) {
        this.eventByCodeRepository = eventByCodeRepository;
        this.cassandraTemplate = cassandraTemplate;
        this.sseService = sseService;
    }

    public List<TimelineEventResponse> findByTrackingCode(String trackingCode) {
        return eventByCodeRepository.findByTrackingCode(trackingCode)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    public TimelineEventResponse save(String trackingCode, EventRequest request) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        EventStatus status = EventStatus.fromId(request.status());

        EventByCode eventByCode = new EventByCode(trackingCode, now, request.state(), request.city(),
                status, request.latitude(), request.longitude(), request.notes());

        EventByCity eventByCity = new EventByCity(request.city(), today, now, trackingCode, status);

        EventByStatus eventByStatus = new EventByStatus(request.status(), today, now, trackingCode, request.city());

        InsertOptions ttlOptions = InsertOptions.builder().ttl(Duration.ofDays(90)).build();

        cassandraTemplate.batchOps()
                .insert(eventByCode, ttlOptions)
                .insert(eventByCity, ttlOptions)
                .insert(eventByStatus, ttlOptions)
                .execute();

        TimelineEventResponse response = TimelineEventResponse.from(eventByCode);
        sseService.publish(trackingCode, response);
        return response;
    }
}
