package team.startup.gwangsan.domain.member.service;

import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;

public interface FindUserInfoService {
    FindUserInfoResponse execute(Long memberId);
}
