package jojo.jjdc.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import jojo.jjdc.config.AppFrontProperties;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.domain.member.OAuthProvider;
import jojo.jjdc.service.MemberService;
import jojo.jjdc.security.oauth.service.GoogleOAuthTokenService;
import jojo.jjdc.security.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;
    private final GoogleOAuthTokenService googleOAuthTokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ObjectMapper objectMapper;
    private final AppFrontProperties appFrontProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new BusinessException(ErrorCode.OAUTH_CLIENT_NOT_FOUND, "지원하지 않는 토큰");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
            token.getAuthorizedClientRegistrationId(),
            token.getName()
        );
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_CLIENT_NOT_FOUND);
        }

        OAuth2User oAuth2User = token.getPrincipal();
        GoogleOAuth2User googleUser = GoogleOAuth2User.fromAttributes(oAuth2User.getAttributes());

        Member member = memberService.upsertMember(OAuthProvider.GOOGLE, googleUser.providerId(), googleUser.email());
        googleOAuthTokenService.upsert(member, client);

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        ResponseCookie refreshCookie = jwtTokenProvider.createRefreshCookie(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, buildRedirectUri(accessToken, member));

        Map<String, Object> payload = Map.of(
            "accessToken", accessToken,
            "memberId", member.getId(),
            "status", member.getStatus().name(),
            "redirectUri", appFrontProperties.getRedirectUri()
        );
        objectMapper.writeValue(response.getWriter(), payload);
        response.flushBuffer();
    }

    private String buildRedirectUri(String accessToken, Member member) {
        String fragment = UriComponentsBuilder.newInstance()
                .queryParam("accessToken", accessToken)
                .queryParam("memberId", member.getId())
                .queryParam("status", member.getStatus().name())
                .build()
                .encode()
                .toUriString();

        if (fragment.startsWith("?")) {
            fragment = fragment.substring(1);
        }

        return UriComponentsBuilder
                .fromUriString(appFrontProperties.getRedirectUri())
                .fragment(fragment)
                .build()
                .toUriString();
    }
}
