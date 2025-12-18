package jojo.jjdc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import jojo.jjdc.dto.NotityDto;
import jojo.jjdc.service.NotityService;
import jojo.jjdc.security.jwt.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar/notities")
@RequiredArgsConstructor
@Tag(name = "Notity", description = "AI 혜택 알람 관리 API")
public class NotityController {

    private final NotityService notityService;

    @GetMapping
    @Operation(summary = "사용자 알람 목록 조회")
    public ResponseEntity<APIResponse<List<NotityDto>>> fetchAll(@AuthenticationPrincipal MemberPrincipal principal) {
        List<NotityDto> notities = notityService.findAll(principal);
        return ResponseEntity
                .status(SuccessCode.NOTITY_FETCHED.getStatus())
                .body(APIResponse.ok(SuccessCode.NOTITY_FETCHED, notities));
    }

    @DeleteMapping("/{notityId}")
    @Operation(summary = "알람 제거")
    public ResponseEntity<APIResponse<Void>> delete(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long notityId
    ) {
        notityService.delete(principal, notityId);
        return ResponseEntity
                .status(SuccessCode.NOTITY_DELETED.getStatus())
                .body(APIResponse.ok(SuccessCode.NOTITY_DELETED));
    }
}
