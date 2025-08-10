package team.startup.gwangsan.domain.member.service;

import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;

import java.util.List;

public interface FindAllUserInfoService {
    List<FindAllUserInfoResponse> execute(String nickname, String placeName);
}

