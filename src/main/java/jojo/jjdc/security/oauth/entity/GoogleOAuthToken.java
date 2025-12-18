package jojo.jjdc.security.oauth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import jojo.jjdc.domain.member.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "google_oauth_tokens")
@NoArgsConstructor
public class GoogleOAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "access_token_expired_at")
    private Instant accessTokenExpiredAt;

    @Column(length = 512)
    private String scope;

    public GoogleOAuthToken(Member member) {
        this.member = member;
    }

    public void updateTokens(String accessToken, Instant expiredAt, String refreshToken, String scope) {
        this.accessToken = accessToken;
        this.accessTokenExpiredAt = expiredAt;
        this.scope = scope;
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
    }
}
