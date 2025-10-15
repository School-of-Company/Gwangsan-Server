package team.startup.gwangsan.domain.member.peresentation.dto.response;

import java.util.List;

public record FindMyInfoResponse(
        Long memberId,
        String nickname,
        String placeName,
        String dongName,
        String headName,
        Integer light,
        Integer gwangsan,
        String description,
        List<String> specialties
) {}
