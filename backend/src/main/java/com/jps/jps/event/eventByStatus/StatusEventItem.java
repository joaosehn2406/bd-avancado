package com.jps.jps.event.eventByStatus;

import java.time.Instant;

public record StatusEventItem(String trackingCode, Instant timestamp, String city) {

    public static StatusEventItem from(EventByStatus e) {
        return new StatusEventItem(e.getTrackingCode(), e.getTimestamp(), e.getCity());
    }
}
