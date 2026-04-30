package team.startup.gwangsan.domain.member.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindAllUserInfoResponse;

public interface FindAllUserInfoService {
    Slice<FindAllUserInfoResponse> execute(String nickname, String placeName, Pageable pageable);
}