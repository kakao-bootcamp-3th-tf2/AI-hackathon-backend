package jojo.jjdc.common.exception;

import jojo.jjdc.common.response.APIResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIResponse<?>> handleBusinessException(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        APIResponse<?> body = APIResponse.error(code, ex.getMessage());
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<APIResponse<?>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String message = ex.getReason();
        if (status != null) {
            if (message == null) {
                message = status.getReasonPhrase();
            }
            return ResponseEntity
                    .status(status)
                    .body(APIResponse.error(status, message));
        }
        String fallbackCode = ex.getStatusCode().toString();
        String fallbackMessage = message != null ? message : fallbackCode;
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(APIResponse.error(fallbackMessage, fallbackCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<?>> handleGenericException(Exception ex) {
        APIResponse<?> body = APIResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
