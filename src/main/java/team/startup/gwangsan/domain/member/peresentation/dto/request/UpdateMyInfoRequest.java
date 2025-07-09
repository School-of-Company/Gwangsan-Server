package team.startup.gwangsan.domain.member.peresentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMyInfoRequest(
        @NotBlank String nickname,
        @NotBlank String profileUrl,
        @NotBlank String description
) {}
