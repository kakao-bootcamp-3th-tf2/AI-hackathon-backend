package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
import jojo.jjdc.security.jwt.MemberPrincipal;
import org.springframework.stereotype.Service;

@Service
public class GoogleCalendarAiService {

    public GoogleCalendarAiResponse requestAiResult(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 요청을 위한 일정 정보가 누락되었습니다.");
        }
        String benefits = describeDummyBenefits(request, principal);
        return new GoogleCalendarAiResponse(request.startAt(), request.endAt(), benefits);
    }

    private String describeDummyBenefits(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        OffsetDateTime start = request.startAt();
        OffsetDateTime end = request.endAt();
        String timespan = start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + " ~ " + end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return String.format("[AI 제안] %s님(%s)의 %s::%s 일정(%s)에 대해 대표 혜택 정보를 준비했습니다.",
                principal != null ? principal.email() : "사용자",
                principal != null ? principal.memberId() : -1,
                request.category(),
                request.brand(),
                timespan);
    }
}
