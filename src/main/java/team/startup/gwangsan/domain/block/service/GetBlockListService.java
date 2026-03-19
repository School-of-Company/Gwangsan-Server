package team.startup.gwangsan.domain.block.service;

import team.startup.gwangsan.domain.block.presentation.dto.response.GetBlockedMemberResponse;

import java.util.List;

public interface GetBlockListService {
    List<GetBlockedMemberResponse> execute();
}
