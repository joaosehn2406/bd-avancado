package com.jps.jps.event.eventByCode;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventByCodeService {

    private final EventByCodeRepository eventByCodeRepository;

    public EventByCodeService(EventByCodeRepository eventByCodeRepository) {
        this.eventByCodeRepository = eventByCodeRepository;
    }

    public List<TimelineEventResponse> findByTrackingCode(String trackingCode) {
        return eventByCodeRepository.findByTrackingCode(trackingCode)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }
}
