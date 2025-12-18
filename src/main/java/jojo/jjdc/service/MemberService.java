package jojo.jjdc.service;

import jojo.jjdc.domain.member.Member;
import jojo.jjdc.domain.member.MemberStatus;
import jojo.jjdc.domain.member.OAuthProvider;
import jojo.jjdc.repository.member.MemberRepository;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member upsertMember(OAuthProvider provider, String providerId, String email) {
        return memberRepository.findByProviderAndProviderId(provider, providerId)
            .map(existing -> {
                existing.updateEmail(email);
                return existing;
            })
            .orElseGet(() -> memberRepository.save(new Member(provider, providerId, email)));
    }

    @Transactional(readOnly = true)
    public Member getById(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Transactional
    public Member updateStatus(Long memberId, MemberStatus status) {
        Member member = getById(memberId);
        member.updateStatus(status);
        return member;
    }
}
