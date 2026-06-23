package com.jps.jps.event.eventByCode;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

public class EventStatusConverter {

    @ReadingConverter
    public static class Read implements Converter<Integer, EventStatus> {
        @Override
        public EventStatus convert(Integer id) {
            return EventStatus.fromId(id);
        }
    }

    @WritingConverter
    public static class Write implements Converter<EventStatus, Integer> {
        @Override
        public Integer convert(EventStatus event) {
            return event.getId();
        }
    }
}
