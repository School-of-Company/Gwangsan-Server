package team.startup.gwangsan.domain.notice.presentation.dto.reqeust;

import jakarta.validation.constraints.NotNull;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;

import java.util.List;

public record CreateNoticeRequest(
        @NotNull String title,
        @NotNull String content,
        @NotNull String placeName,
        List<Long> imageIds,
        List<MemberRole> roles
) {}