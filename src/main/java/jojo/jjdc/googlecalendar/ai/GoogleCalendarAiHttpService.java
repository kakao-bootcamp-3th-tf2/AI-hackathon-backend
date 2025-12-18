package jojo.jjdc.googlecalendar.ai;

import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.ai.AiGatewayService;
import jojo.jjdc.ai.dto.AiPlanPayload;
import jojo.jjdc.ai.dto.AiRecommendationItem;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarAppendRequest;
import jojo.jjdc.googlecalendar.dto.GoogleCalendarCreateEventRequest;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleCalendarAiHttpService implements GoogleCalendarAiService {

    private final AiGatewayService aiGatewayService;
    private final MemberRepository memberRepository;

    @Override
    public GoogleCalendarAiResponse requestAiResult(GoogleCalendarCreateEventRequest request, MemberPrincipal principal) {
        Member member = loadMember(principal);
        AiPlanPayload plan = new AiPlanPayload(request.startAt(), request.brand(), request.category());
        List<AiRecommendationItem> items = aiGatewayService.requestRecommendations(member, plan);
        String description = formatDescription(request.category(), request.brand(), items, "새 일정");
        return new GoogleCalendarAiResponse(
                request.startAt(),
                request.endAt(),
                description,
                items
        );
    }

    @Override
    public GoogleCalendarAiResponse requestAppendAiResult(
            String eventId,
            String eventSummary,
            OffsetDateTime start,
            OffsetDateTime end,
            GoogleCalendarAppendRequest request,
            MemberPrincipal principal
    ) {
        Member member = loadMember(principal);
        AiPlanPayload plan = new AiPlanPayload(start, request.brand(), request.category());
        List<AiRecommendationItem> items = aiGatewayService.requestRecommendations(member, plan);
        String description = formatDescription(request.category(), request.brand(), items, eventSummary);
        return new GoogleCalendarAiResponse(
                start,
                end,
                description,
                items
        );
    }

    private Member loadMember(MemberPrincipal principal) {
        return memberRepository.findById(principal.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private String formatDescription(String category, String brand, List<AiRecommendationItem> items, String context) {
        StringBuilder builder = new StringBuilder();
        builder.append("카테고리: ").append(category).append("\n");
        builder.append("브랜드: ").append(brand).append("\n");
        builder.append("AI 추천 요약: ").append(context).append("\n");
        if (items == null || items.isEmpty()) {
            builder.append("현재 추천할 혜택이 없습니다.");
            return builder.toString();
        }
        builder.append("추천 혜택 안내:\n");
        for (int i = 0; i < items.size(); i++) {
            AiRecommendationItem item = items.get(i);
            builder.append(i + 1).append(". ").append(item.message());
            if (item.startAt() != null || item.endAt() != null) {
                builder.append(" (")
                        .append(item.startAt() != null ? item.startAt() : "-")
                        .append(" ~ ")
                        .append(item.endAt() != null ? item.endAt() : "-")
                        .append(")");
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}
