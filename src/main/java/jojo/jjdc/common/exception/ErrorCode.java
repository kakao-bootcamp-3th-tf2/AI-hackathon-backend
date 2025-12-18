package jojo.jjdc.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "Access Token이 유효하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Refresh Token이 유효하지 않습니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "회원 정보를 찾을 수 없습니다."),

    OAUTH_CLIENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "OAUTH_001", "OAuth 클라이언트가 존재하지 않습니다."),

    GOOGLE_TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "GOOGLE_001", "구글 연동 토큰을 찾을 수 없습니다."),
    GOOGLE_REFRESH_TOKEN_MISSING(HttpStatus.BAD_REQUEST, "GOOGLE_002", "구글 refresh token이 존재하지 않습니다. 재동의가 필요합니다."),
    GOOGLE_CALENDAR_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "GOOGLE_003", "구글 캘린더 요청이 실패했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
