package jojo.jjdc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MemberJoinRequest(
        @Schema(description = "회원 ID", example = "1")
        @NotNull
        Long memberId,

        @Schema(description = "이용 중인 통신사", example = "SKT")
        @NotBlank
        String telecom,

        @Schema(description = "사용 중인 결제 수단 리스트", example = "[\"삼성페이\", \"네이버페이\"]")
        @NotEmpty
        List<@NotBlank String> payments
) {
}
