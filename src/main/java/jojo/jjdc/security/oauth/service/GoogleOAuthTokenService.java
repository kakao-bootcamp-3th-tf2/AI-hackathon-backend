package jojo.jjdc.security.oauth.service;

import java.time.Instant;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.security.oauth.entity.GoogleOAuthToken;
import jojo.jjdc.security.oauth.repository.GoogleOAuthTokenRepository;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoogleOAuthTokenService {

    private final GoogleOAuthTokenRepository repository;

    @Transactional
    public GoogleOAuthToken upsert(Member member, OAuth2AuthorizedClient client) {
        GoogleOAuthToken token = repository.findByMemberId(member.getId())
            .orElseGet(() -> new GoogleOAuthToken(member));

        String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;
        // TODO: 운영 전환 시 refresh token 암호화 저장
        String scope = client.getAccessToken().getScopes() != null ? String.join(" ", client.getAccessToken().getScopes()) : null;
        token.updateTokens(
            client.getAccessToken().getTokenValue(),
            client.getAccessToken().getExpiresAt() != null ? client.getAccessToken().getExpiresAt() : Instant.now().plusSeconds(3600),
            refreshToken,
            scope
        );
        return repository.save(token);
    }

    @Transactional(readOnly = true)
    public GoogleOAuthToken getByMemberId(Long memberId) {
        return repository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GOOGLE_TOKEN_NOT_FOUND));
    }

    @Transactional
    public GoogleOAuthToken updateAccessToken(GoogleOAuthToken token, String accessToken, Instant expiresAt) {
        token.updateTokens(accessToken, expiresAt, null, token.getScope());
        return repository.save(token);
    }
}
