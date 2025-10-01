package team.startup.gwangsan.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(

        @Pattern(regexp = "^010\\d{8}$", message = "올바른 휴대폰 번호 형식이어야 합니다.")
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber,

        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword
) {
}
