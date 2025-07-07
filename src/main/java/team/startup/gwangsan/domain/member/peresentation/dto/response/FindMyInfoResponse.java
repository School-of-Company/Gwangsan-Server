package team.startup.gwangsan.domain.member.peresentation.dto.response;

public record FindMyInfoResponse(
        Long memberId,
        String nickname,
        String placeName,
        String profileUrl,
        Integer light,
        Integer gwangsan
) {}

