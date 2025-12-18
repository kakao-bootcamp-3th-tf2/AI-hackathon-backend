package jojo.jjdc.security.provider;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import jojo.jjdc.domain.member.Member;
import jojo.jjdc.service.member.MemberService;
import jojo.jjdc.security.jwt.MemberPrincipal;
import jojo.jjdc.security.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import jojo.jjdc.common.exception.BusinessException;
import jojo.jjdc.common.exception.ErrorCode;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	// Header
	public static final String ACCESS_HEADER = "Authorization";
	public static final String BEARER_PREFIX = "Bearer ";

	// Cookie
	public static final String REFRESH_COOKIE = "refresh_token";

	// Claim
	private static final String CLAIM_TOKEN_TYPE = "typ";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	// Path
	public static final String REFRESH_PATH = "/api/auth/token";

    private final JwtProperties props;
    private final MemberService memberService;

	private SecretKey accessKey;
	private SecretKey refreshKey;

	@PostConstruct
	void init() {
		this.accessKey = Keys.hmacShaKeyFor(
			props.getAccess().secret().getBytes(StandardCharsets.UTF_8)
		);
		this.refreshKey = Keys.hmacShaKeyFor(
			props.getRefresh().secret().getBytes(StandardCharsets.UTF_8)
		);
	}

	/*
	토큰 생성
	- AT, RT 생성
	- RT HttpOnly Cookie 생성
	- 인증 객체 생성
	 */

	public String createAccessToken(Long userId) {
		return createToken(userId, TYPE_ACCESS, accessKey, props.getAccess().ttl());
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, TYPE_REFRESH, refreshKey, props.getRefresh().ttl());
	}

	private String createToken(Long userId, String type, SecretKey key, Duration ttl) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + ttl.toMillis());

		return Jwts.builder()
				.subject(String.valueOf(userId))
				.issuedAt(now)
				.expiration(exp)
				.claim(CLAIM_TOKEN_TYPE, type)
				.signWith(key, Jwts.SIG.HS256)
				.compact();
	}

	public ResponseCookie createRefreshCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
				.path(REFRESH_PATH)
				.httpOnly(true)
				.secure(true) // TODO 운영 시 HTTPS 배포 전까지 false 고려
				.sameSite("None") // TODO 운영 시 FE 배포 도메인에 맞춰 재검토
				.maxAge(props.getRefresh().ttl())
				.build();
	}

	public Authentication getAuthentication(String token) {
		Claims claims = parseClaims(token, accessKey, ErrorCode.INVALID_ACCESS_TOKEN).getPayload();
		if (!TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
			throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
		}
		Long memberId = Long.valueOf(claims.getSubject());
		Member member = memberService.getById(memberId);
		MemberPrincipal principal = MemberPrincipal.from(member);
		return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
	}

	/*
	토큰 검증
	 */
	public boolean isAccessToken(String token) {
		return hasType(token, accessKey, TYPE_ACCESS);
	}

	public boolean isRefreshToken(String token) {
		return hasType(token, refreshKey, TYPE_REFRESH);
	}

	private boolean hasType(String token, SecretKey key, String type) {
		try {
			Claims claims = parseClaims(token, key, errorCodeForType(type)).getPayload();
			return type.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
		} catch (BusinessException ex) {
			return false;
		}
	}

	public Long extractMemberIdFromRefresh(String token) {
		Claims claims = parseClaims(token, refreshKey, ErrorCode.INVALID_REFRESH_TOKEN).getPayload();
		return Long.valueOf(claims.getSubject());
	}

	private Jws<Claims> parseClaims(String token, SecretKey key, ErrorCode errorCode) {
		try {
			return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
		} catch (JwtException ex) {
			throw new BusinessException(errorCode, ex.getMessage());
		}
	}

	private ErrorCode errorCodeForType(String type) {
		return TYPE_REFRESH.equals(type) ? ErrorCode.INVALID_REFRESH_TOKEN : ErrorCode.INVALID_ACCESS_TOKEN;
	}
}
