package team.startup.gwangsan.domain.member.peresentation.dto.response;

public record GetMyInfoResponse(
        Long memberId,
        String nickname,
        String profileUrl,
        Integer light,
        Integer gwangsan
) {}

