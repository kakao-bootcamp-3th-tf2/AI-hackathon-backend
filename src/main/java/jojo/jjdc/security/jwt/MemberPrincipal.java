package jojo.jjdc.security.jwt;

import java.util.Collection;
import java.util.List;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.domain.member.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record MemberPrincipal(Long memberId, String email, MemberStatus status) implements UserDetails {

    public static MemberPrincipal from(Member member) {
        return new MemberPrincipal(member.getId(), member.getEmail(), member.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = status == MemberStatus.ACTIVE ? "ROLE_USER" : "ROLE_PENDING";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
