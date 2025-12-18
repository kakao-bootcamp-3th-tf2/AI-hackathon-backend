package jojo.jjdc.api.health;

import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<APIResponse<HealthCheckResponse>> health() {
        return ResponseEntity
                .status(SuccessCode.HEALTH_CHECK.getStatus())
                .body(APIResponse.ok(SuccessCode.HEALTH_CHECK, HealthCheckResponse.up()));
    }
}
