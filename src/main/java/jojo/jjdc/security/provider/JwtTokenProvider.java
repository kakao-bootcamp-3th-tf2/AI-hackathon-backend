package jojo.jjdc.security.provider;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jojo.jjdc.security.properties.JwtProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	// Header
	private static final String ACCESS_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	// Cookie
	public static final String REFRESH_COOKIE = "refresh_token";

	// Claim
	private static final String CLAIM_TOKEN_TYPE = "typ";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	// Path
	public static final String REFRESH_PATH = "/auth/refresh";

    private final JwtProperties props;
    private final UserDetailsService userDetailsService;

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

	public String createAccessToken(Long userId) {
		return createToken(userId, TYPE_ACCESS, accessKey, props.getAccess().ttl());
	}

	public String createRefreshToken(Long userId) {
		return createToken(userId, TYPE_REFRESH, refreshKey, props.getRefresh().ttl());
	}

	public ResponseCookie createRefreshCookie(String refreshToken) {
		return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
			.path(REFRESH_PATH)
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.maxAge(props.getRefresh().ttl())
			.build();
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
}
