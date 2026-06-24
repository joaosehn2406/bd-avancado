package com.jps.jps.event.eventByStatus;

import com.jps.jps.event.eventByCode.EventStatusResponse;

import java.time.LocalDate;
import java.util.List;

public record StatusActivityResponse(EventStatusResponse status, LocalDate date, List<StatusEventItem> events) {}
