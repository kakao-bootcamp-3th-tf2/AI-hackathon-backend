package jojo.jjdc.common.response;

import jojo.jjdc.common.exception.ErrorCode;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
@AllArgsConstructor
public class APIResponse<T> {

    private final String message;
    private final String code;
    private final T data;

    public static <T> APIResponse<T> ok(SuccessCode code, T data) {
        return new APIResponse<>(code.getMessage(), code.getCode(), data);
    }

    public static <T> APIResponse<T> ok(SuccessCode code) {
        return ok(code, null);
    }

    public static APIResponse<?> error(ErrorCode code, String message) {
        String finalMessage = message != null ? message : code.getMessage();
        return new APIResponse<>(finalMessage, code.getCode(), Map.of());
    }

    public static APIResponse<?> error(ErrorCode code) {
        return error(code, null);
    }

    public static APIResponse<?> error(String message, String code) {
        return new APIResponse<>(message, code, Map.of());
    }

    public static APIResponse<?> error(HttpStatus status) {
        return error(status.getReasonPhrase(), status.name());
    }

    public static APIResponse<?> error(HttpStatus status, String message) {
        return error(message, status.name());
    }
}
