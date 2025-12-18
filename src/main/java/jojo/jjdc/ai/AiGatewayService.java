package jojo.jjdc.ai;

import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.config.AppAiProperties;
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
}
