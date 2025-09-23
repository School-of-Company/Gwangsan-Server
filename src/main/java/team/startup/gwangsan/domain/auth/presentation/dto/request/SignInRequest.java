package team.startup.gwangsan.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import team.startup.gwangsan.domain.auth.entity.constant.OsType;

public record SignInRequest(

        @NotBlank
        @Pattern(
                regexp = "^[가-힣a-zA-Z0-9 ()~]+$",
                message = "닉네임은 한글, 영문, 숫자, 공백, (), ~ 만 입력 가능합니다."
        )
        String nickname,

        @NotBlank
        String password,

        @NotBlank
        String deviceToken,

        @NotBlank
        String deviceId,

        OsType osType

) {}
