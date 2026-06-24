package com.jps.jps.event.eventByCity;

import java.time.LocalDate;
import java.util.List;

public record CityActivityResponse(String city, LocalDate date, List<CityEventItem> events) {}
