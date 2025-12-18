package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarAppendRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
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
