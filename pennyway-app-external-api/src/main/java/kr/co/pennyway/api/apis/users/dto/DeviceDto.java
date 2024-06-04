package kr.co.pennyway.api.apis.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kr.co.pennyway.domain.domains.device.domain.Device;
import kr.co.pennyway.domain.domains.user.domain.User;

public class DeviceDto {
    @Schema(title = "디바이스 등록 요청")
    public record RegisterReq(
            @Schema(description = "기존 디바이스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "originToken은 필수입니다.")
            String originToken,
            @Schema(description = "새로운 디바이스 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "newToken은 필수입니다.")
            String newToken
    ) {
        /**
         * oldToken과 newToken이 같은 경우, 신규 등록 요청으로 판단
         */
        @Schema(hidden = true)
        public boolean isInitRequest() {
            return originToken.equals(newToken);
        }

        public Device toEntity(User user) {
            return Device.of(newToken, user);
        }
    }

    @Schema(title = "디바이스 등록 응답")
    public record RegisterRes(
            @Schema(title = "디바이스 ID")
            Long id,
            @Schema(title = "디바이스 토큰")
            String token
    ) {
        public static RegisterRes of(Long id, String token) {
            return new RegisterRes(id, token);
        }
    }
}
