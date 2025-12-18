package jojo.jjdc.googlecalendar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Google Calendar에서 가져온 이벤트 리스트")
public record GoogleCalendarEventsResponse(
        List<GoogleCalendarEventDto> events
) { }
