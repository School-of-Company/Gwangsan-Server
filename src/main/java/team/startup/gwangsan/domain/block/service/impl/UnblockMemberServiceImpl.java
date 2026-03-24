package team.startup.gwangsan.domain.block.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.block.service.UnblockMemberService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class UnblockMemberServiceImpl implements UnblockMemberService {

    private final MemberUtil memberUtil;
    private final MemberBlockRepository memberBlockRepository;

    @Override
    @Transactional
    public void execute(Long targetMemberId) {
        Member currentMember = memberUtil.getCurrentMember();

        MemberBlock block = memberBlockRepository
                .findByBlockerIdAndBlockedId(currentMember.getId(), targetMemberId)
                .orElseThrow(NotFoundBlockException::new);

        memberBlockRepository.delete(block);
    }
}
