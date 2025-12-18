package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.googlecalendar.ai.AiServerResponse.AiSuggestion;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarAppendRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
import jojo.jjdc.security.jwt.MemberPrincipal;
import org.springframework.stereotype.Service;

@Service
public class GoogleCalendarAiDummyService implements GoogleCalendarAiService {

    public GoogleCalendarAiResponse requestAiResult(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        AiServerResponse payload = simulateAiResponse(request, principal);
        String text = formatForCalendar(payload);
        return new GoogleCalendarAiResponse(request.startAt(), request.endAt(), text, payload.data());
    }

    public GoogleCalendarAiResponse requestAppendAiResult(
            String eventId,
            String eventSummary,
            OffsetDateTime start,
            OffsetDateTime end,
            GoogleCalendarAppendRequest request,
            MemberPrincipal principal
    ) {
        if (eventId == null || eventId.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 요청을 위한 eventId가 누락되었습니다.");
        }
        AiServerResponse payload = simulateAiResponseForAppend(request, principal);
        String body = formatForCalendar(payload, eventSummary);
        return new GoogleCalendarAiResponse(start != null ? start : OffsetDateTime.now(), end != null ? end : OffsetDateTime.now(), body, payload.data());
    }

    private String describeDummyBenefits(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        if (request == null) {
            return "AI 혜택 정보는 준비되지 않았습니다.";
        }
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

    private String formatForCalendar(AiServerResponse response) {
        return formatForCalendar(response, null);
    }

    private String formatForCalendar(AiServerResponse response, String context) {
        StringBuilder builder = new StringBuilder();
        if (context != null) {
            builder.append("기존 일정: ").append(context).append("\n");
        }
        builder.append("AI 응답: ").append(response.message()).append(" (code=").append(response.code()).append(")\n");
        List<AiSuggestion> suggestions = response.data();
        if (suggestions == null) {
            builder.append("⚠️ AI 추천이 정상적으로 처리되지 않았습니다. 다시 시도해 주세요.");
            return builder.toString();
        }
        if (suggestions.isEmpty()) {
            builder.append("현재 추천할 혜택이 없습니다.");
            return builder.toString();
        }
        builder.append("추천 일정 안내:\n");
        for (int i = 0; i < suggestions.size(); i++) {
            AiSuggestion suggestion = suggestions.get(i);
            builder.append(i + 1).append(". ").append(suggestion.suggest());
            if (suggestion.fromDate() != null && suggestion.toDate() != null) {
                builder.append(" (").append(suggestion.fromDate()).append(" ~ ").append(suggestion.toDate()).append(")");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private AiServerResponse simulateAiResponse(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        if (request == null) {
            return new AiServerResponse("Request body is null", "AI_400", null);
        }
        AiSuggestion suggestion = new AiSuggestion(
                "이벤트 내용 기반 혜택 안내",
                request.startAt().toString(),
                request.endAt().toString()
        );
        return new AiServerResponse(
                "성공적으로 혜택을 생성했습니다",
                "AI_200",
                List.of(suggestion)
        );
    }

    private AiServerResponse simulateAiResponseForAppend(GoogleCalendarAppendRequest request, MemberPrincipal principal) {
        String brand = request != null && request.brand() != null ? request.brand() : "알 수 없는 브랜드";
        String category = request != null && request.category() != null ? request.category() : "기본";
        AiSuggestion suggestion = new AiSuggestion(
                String.format("%s(%s) 관련 AI 요약이 추가되었습니다", brand, category),
                null,
                null
        );
        return new AiServerResponse(
                "기존 일정에 AI 메모를 추가했습니다",
                "AI_200",
                List.of(suggestion)
        );
    }
}
