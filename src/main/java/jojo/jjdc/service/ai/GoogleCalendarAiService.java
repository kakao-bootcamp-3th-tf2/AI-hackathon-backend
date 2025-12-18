package jojo.jjdc.service.ai;

import java.time.OffsetDateTime;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarAppendRequest;
import jojo.jjdc.dto.googlecalendar.GoogleCalendarCreateEventRequest;
import jojo.jjdc.security.jwt.MemberPrincipal;

public interface GoogleCalendarAiService {

    GoogleCalendarAiResponse requestAiResult(GoogleCalendarCreateEventRequest request, MemberPrincipal principal);

    GoogleCalendarAiResponse requestAppendAiResult(
            String eventId,
            String eventSummary,
            OffsetDateTime start,
            OffsetDateTime end,
            GoogleCalendarAppendRequest request,
            MemberPrincipal principal
    );
}
