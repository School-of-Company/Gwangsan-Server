package team.startup.gwangsan.adapter.web.member.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateMyInfoRequest(
        @NotBlank String nickname,
        @NotBlank String description,
        List<@NotBlank String> specialties
) {}
