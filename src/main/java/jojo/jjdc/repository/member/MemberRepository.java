package jojo.jjdc.repository.member;

import java.util.Optional;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.domain.member.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
