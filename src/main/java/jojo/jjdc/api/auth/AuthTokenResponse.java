package jojo.jjdc.api.auth;

import jojo.jjdc.domain.member.MemberStatus;

public record AuthTokenResponse(String accessToken, MemberStatus status) {
}
