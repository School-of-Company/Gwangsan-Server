package team.startup.gwangsan.domain.member.peresentation.dto.response;

import java.util.List;

public record FindUserInfoResponse(
        Long memberId,
        String nickname,
        String profileUrl,
        String placeName,
        Integer light,
        String description,
        List<String> specialties
) {}
