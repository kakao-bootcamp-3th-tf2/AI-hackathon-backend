package jojo.jjdc.ai;

import java.time.OffsetDateTime;
import java.util.List;
import jojo.jjdc.ai.dto.AiPlanPayload;
import jojo.jjdc.ai.dto.AiRecommendationItem;
import jojo.jjdc.ai.dto.AiRecommendationRequest;
import jojo.jjdc.ai.dto.AiRecommendationResponse;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.config.AppAiProperties;
import jojo.jjdc.domain.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AiGatewayService {

    private final RestTemplate restTemplate;
    private final AppAiProperties appAiProperties;

    public String fetchHealth() {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(appAiProperties.getHealthUrl(), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED,
                        "헬스 응답 코드: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED, ex.getMessage());
        }
    }

    public List<AiRecommendationItem> requestRecommendations(Member member, AiPlanPayload plan) {
        try {
            AiRecommendationRequest request = new AiRecommendationRequest(
                    new AiRecommendationRequest.UserPayload(member.getTelecom(), safePayments(member)),
                    new AiRecommendationRequest.PlanPayload(
                            formatDateTime(plan.dateTime()),
                            plan.brand(),
                            plan.category()
                    )
            );
            ResponseEntity<AiRecommendationResponse> response =
                    restTemplate.postForEntity(appAiProperties.getRecommendUrl(), request, AiRecommendationResponse.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED,
                        "추천 응답 코드: " + response.getStatusCode());
            }
            AiRecommendationResponse body = response.getBody();
            return body.data() != null ? body.data() : List.of();
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED, ex.getMessage());
        }
    }

    private List<String> safePayments(Member member) {
        return member.getPayments() != null ? member.getPayments() : List.of();
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        OffsetDateTime target = dateTime != null ? dateTime : OffsetDateTime.now();
        return target.toString();
    }
}
