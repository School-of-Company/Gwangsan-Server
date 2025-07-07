package team.startup.gwangsan.domain.member.peresentation.dto.response;

public record FindUserInfoResponse(
        Long memberId,
        String nickname,
        String profileUrl,
        String placeName,
        Integer light
) {}
