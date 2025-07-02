package team.startup.gwangsan.domain.member.peresentation.dto.response;

public record FindAllUserInfoResponse(
        Long memberId,
        String nickname,
        String profileUrl,
        Integer light,
        Integer gwangsan
) {}

