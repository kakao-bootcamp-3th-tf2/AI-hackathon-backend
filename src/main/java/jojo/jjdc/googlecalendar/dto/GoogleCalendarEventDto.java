package jojo.jjdc.googlecalendar.dto;

import java.time.Instant;

public record GoogleCalendarEventDto(String id, String summary, Instant start, Instant end) {
}
