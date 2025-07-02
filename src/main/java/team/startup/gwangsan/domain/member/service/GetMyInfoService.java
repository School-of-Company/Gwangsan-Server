package team.startup.gwangsan.domain.member.service;

import jakarta.servlet.http.HttpServletRequest;
import team.startup.gwangsan.domain.member.peresentation.dto.response.GetMyInfoResponse;

public interface GetMyInfoService {
    GetMyInfoResponse execute(HttpServletRequest request);
}

