package team.startup.gwangsan.domain.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SignUpRequest(

        @NotBlank
        String name,

        @Pattern(regexp = "^[가-힣]+$", message = "닉네임은 한글만 가능합니다.")
        String nickname,

        @NotBlank
        String password,

        @Pattern(regexp = "^010\\d{8}$", message = "올바른 휴대폰 번호 형식이어야 합니다.")
        String phoneNumber,

        @NotBlank(message = "동은 필수입니다.")
        Integer dongName,

        @NotNull
        Integer placeId,

        @NotEmpty(message = "특기는 한 개 이상 선택해야 합니다.")
        List<@NotBlank String> specialties,

        @Pattern(regexp = "^[가-힣]+$", message = "추천인은 한글만 가능합니다.")
        @NotBlank(message = "추천인은 필수입니다.")
        String recommender

) {}
