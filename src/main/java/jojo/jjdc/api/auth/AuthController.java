package jojo.jjdc.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import jojo.jjdc.common.response.APIResponse;
import jojo.jjdc.common.response.SuccessCode;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.security.provider.JwtTokenProvider;
import jojo.jjdc.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "우리 서비스 JWT 상태 조회 및 재발급 API")
public class AuthController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/status")
    @Operation(summary = "로그인 상태 조회", description = "Access Token으로 현재 회원 정보와 온보딩 상태를 조회한다.")
    public ResponseEntity<APIResponse<AuthStatusResponse>> getStatus(@AuthenticationPrincipal MemberPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberService.getById(principal.memberId());
        AuthStatusResponse response = AuthStatusResponse.from(member);
        return ResponseEntity
                .status(SuccessCode.AUTH_STATUS_FETCHED.getStatus())
                .body(APIResponse.ok(SuccessCode.AUTH_STATUS_FETCHED, response));
    }

    @PostMapping("/token")
    @Operation(summary = "Access Token 재발급", description = "Refresh Token(HttpOnly 쿠키)을 이용해 Access Token/Refresh Token을 재발급한다.")
    public ResponseEntity<APIResponse<AuthTokenResponse>> refresh(
            @CookieValue(name = JwtTokenProvider.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        if (!StringUtils.hasText(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = jwtTokenProvider.extractMemberIdFromRefresh(refreshToken);
        Member member = memberService.getById(memberId);

        String newAccessToken = jwtTokenProvider.createAccessToken(memberId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(memberId);
        ResponseCookie refreshCookie = jwtTokenProvider.createRefreshCookie(newRefreshToken);

        AuthTokenResponse response = new AuthTokenResponse(newAccessToken, member.getStatus());

        return ResponseEntity
                .status(SuccessCode.AUTH_TOKEN_REFRESHED.getStatus())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(APIResponse.ok(SuccessCode.AUTH_TOKEN_REFRESHED, response));
    }
}
