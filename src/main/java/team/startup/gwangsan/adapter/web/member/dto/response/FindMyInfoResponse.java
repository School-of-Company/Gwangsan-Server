package team.startup.gwangsan.adapter.web.member.dto.response;

import java.util.List;

public record FindMyInfoResponse(
        Long memberId,
        String nickname,
        Integer placeId,
        String placeName,
        String dongName,
        String headName,
        Integer light,
        Integer gwangsan,
        String description,
        List<String> specialties
) {}
