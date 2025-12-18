package jojo.jjdc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AppAiProperties {

    /**
     * AI 서버의 FastAPI 엔드포인트.
     */
    private String baseUrl = "http://localhost:8000";

    /**
     * AI 서버에서 제공하는 헬스체크 URL.
     */
    private String healthUrl = "http://localhost:8080/health";
}
