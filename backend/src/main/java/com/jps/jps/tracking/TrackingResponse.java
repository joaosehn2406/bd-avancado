package com.jps.jps.tracking;

import com.jps.jps.event.eventByCode.EventStatusResponse;
import com.jps.jps.event.eventByCode.TimelineEventResponse;

import java.time.Instant;
import java.util.List;

public record TrackingResponse(
        String trackingCode,
        String origin,
        String destination,
        Instant createdAt,
        EventStatusResponse currentStatus,
        List<TimelineEventResponse> events
) {}
