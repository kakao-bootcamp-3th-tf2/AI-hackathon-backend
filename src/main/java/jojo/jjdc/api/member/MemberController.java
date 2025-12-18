package jojo.jjdc.api.member;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import jojo.jjdc.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 프로필/온보딩 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/join")
    @Operation(summary = "소셜 로그인 사용자 온보딩 완료", description = "통신사/결제 수단 정보를 입력하고 회원 상태를 ACTIVE로 전환합니다.")
    public ResponseEntity<APIResponse<Void>> join(@Valid @RequestBody MemberJoinRequest request) {
        memberService.updateProfile(request.memberId(), request.telecom(), request.payments());
        return ResponseEntity
                .status(SuccessCode.MEMBER_PROFILE_UPDATED.getStatus())
                .body(APIResponse.ok(SuccessCode.MEMBER_PROFILE_UPDATED));
    }
}
