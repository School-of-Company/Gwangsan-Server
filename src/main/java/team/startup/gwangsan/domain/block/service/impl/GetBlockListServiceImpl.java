package team.startup.gwangsan.domain.block.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.block.presentation.dto.response.GetBlockedMemberResponse;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.block.service.GetBlockListService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBlockListServiceImpl implements GetBlockListService {

    private final MemberUtil memberUtil;
    private final MemberBlockRepository memberBlockRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetBlockedMemberResponse> execute() {
        Member currentMember = memberUtil.getCurrentMember();

        return memberBlockRepository.findAllByBlocker(currentMember)
                .stream()
                .map(block -> new GetBlockedMemberResponse(
                        block.getBlocked().getId(),
                        block.getBlocked().getNickname()
                ))
                .toList();
    }
}
