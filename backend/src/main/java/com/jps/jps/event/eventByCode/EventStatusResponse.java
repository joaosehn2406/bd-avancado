package com.jps.jps.event.eventByCode;

public record EventStatusResponse(
        Integer id,
        String name
) {
    public static EventStatusResponse from(EventStatus status) {
        return new EventStatusResponse(status.getId(), status.getName());
    }
}
