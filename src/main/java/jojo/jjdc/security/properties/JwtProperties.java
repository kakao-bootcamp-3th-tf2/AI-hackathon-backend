package jojo.jjdc.security.properties;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
	Token access;
	Token refresh;

	public record Token(
		String secret,
		Duration ttl
	) {}
}
