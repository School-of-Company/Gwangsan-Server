package team.startup.gwangsan.domain.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;

@NoArgsConstructor
@Getter
public class SignUpRequest {

    @NotBlank
    private String name;

    @Pattern(regexp = "^[가-힣]+$", message = "닉네임은 한글만 가능합니다.")
    private String nickname;

    @NotBlank
    private String password;

    @Pattern(regexp = "^010\\d{8}$", message = "올바른 휴대폰 번호 형식이어야 합니다.")
    private String phoneNumber;

    @NotNull
    private Integer dongId;

    @NotNull
    private Integer placeId;

    @NotNull
    private RelatedKeyword specialty;

    private String recommender;
}
