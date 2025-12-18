package jojo.jjdc.security.oauth.repository;

import java.util.Optional;
import jojo.jjdc.security.oauth.entity.GoogleOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, Long> {
    Optional<GoogleOAuthToken> findByMemberId(Long memberId);
}
