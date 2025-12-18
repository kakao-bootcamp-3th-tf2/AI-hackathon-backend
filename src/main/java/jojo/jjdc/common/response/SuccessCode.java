package jojo.jjdc.common.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode {
    AUTH_STATUS_FETCHED(HttpStatus.OK, "AUTH_200", "로그인 상태 조회에 성공했습니다."),
    AUTH_TOKEN_REFRESHED(HttpStatus.OK, "AUTH_201", "Access/Refresh Token을 재발급했습니다."),
    GOOGLE_EVENTS_FETCHED(HttpStatus.OK, "GOOGLE_200", "구글 캘린더 이벤트 조회에 성공했습니다."),
    HEALTH_CHECK(HttpStatus.OK, "HEALTH_200", "서비스가 정상적으로 동작 중입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
