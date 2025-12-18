package jojo.jjdc.api.auth;

import jojo.jjdc.domain.member.Member;
import jojo.jjdc.domain.member.MemberStatus;

public record AuthStatusResponse(
        Long memberId,
        MemberStatus status,
        String email
) {
    public static AuthStatusResponse from(Member member) {
        return new AuthStatusResponse(member.getId(), member.getStatus(), member.getEmail());
    }
}
