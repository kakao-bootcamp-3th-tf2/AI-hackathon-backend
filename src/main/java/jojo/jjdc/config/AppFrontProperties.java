package jojo.jjdc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.front")
public class AppFrontProperties {

    /**
     * OAuth 인증 후 리디렉션할 프런트 페이지 URI.
     * 기본값은 백엔드에서 제공하는 데모 페이지를 가리킨다.
     */
    private String redirectUri = "http://localhost:8080/demo/oauth2-redirect.html";
}
