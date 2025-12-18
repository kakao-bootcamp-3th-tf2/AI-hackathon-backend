package jojo.jjdc.api.health;

import jojo.jjdc.ai.AiGatewayService;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final AiGatewayService aiGatewayService;

    @GetMapping
    public ResponseEntity<APIResponse<HealthCheckResponse>> health() {
        return ResponseEntity
                .status(SuccessCode.HEALTH_CHECK.getStatus())
                .body(APIResponse.ok(SuccessCode.HEALTH_CHECK, HealthCheckResponse.up()));
    }

    @GetMapping("/ai")
    public ResponseEntity<APIResponse<String>> aiHealth() {
        String body = aiGatewayService.fetchHealth();
        return ResponseEntity
                .status(SuccessCode.AI_HEALTH_FETCHED.getStatus())
                .body(APIResponse.ok(SuccessCode.AI_HEALTH_FETCHED, body));
    }
}
