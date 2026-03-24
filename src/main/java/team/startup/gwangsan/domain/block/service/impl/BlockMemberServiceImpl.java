package team.startup.gwangsan.domain.block.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.block.service.BlockMemberService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class BlockMemberServiceImpl implements BlockMemberService {

    private final MemberUtil memberUtil;
    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    @Override
    @Transactional
    public void execute(Long targetMemberId) {
        Member currentMember = memberUtil.getCurrentMember();

        if (currentMember.getId().equals(targetMemberId)) {
            throw new SelfBlockNotAllowedException();
        }

        if (memberBlockRepository.existsByBlockerIdAndBlockedId(currentMember.getId(), targetMemberId)) {
            throw new AlreadyBlockedException();
        }

        Member targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(NotFoundMemberException::new);

        memberBlockRepository.save(MemberBlock.builder()
                .blocker(currentMember)
                .blocked(targetMember)
                .build());
    }
}
