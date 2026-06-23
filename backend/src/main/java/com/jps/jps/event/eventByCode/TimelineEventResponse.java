package com.jps.jps.event.eventByCode;

import java.time.Instant;

public record TimelineEventResponse(
        Instant timestamp,
        String state,
        String city,
        EventStatusResponse status,
        Double latitude,
        Double longitude,
        String notes
) {
    public static TimelineEventResponse from(EventByCode event) {
        return new TimelineEventResponse(
                event.getTimestamp(),
                event.getState(),
                event.getCity(),
                EventStatusResponse.from(event.getStatus()),
                event.getLatitude(),
                event.getLongitude(),
                event.getNotes()
        );
    }
}
