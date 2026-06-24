package com.jps.jps.event.eventByCity;

import com.jps.jps.event.eventByCode.EventStatusResponse;

import java.time.Instant;

public record CityEventItem(String trackingCode, Instant timestamp, EventStatusResponse status) {

    public static CityEventItem from(EventByCity e) {
        return new CityEventItem(e.getTrackingCode(), e.getTimestamp(), EventStatusResponse.from(e.getStatus()));
    }
}
