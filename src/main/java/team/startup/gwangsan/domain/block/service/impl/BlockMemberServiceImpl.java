package team.startup.gwangsan.domain.block.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.block.service.BlockMemberService;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class BlockMemberServiceImpl implements BlockMemberService {

    private final MemberUtil memberUtil;
    private final MemberJpaRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    @Override
    @Transactional
    public void execute(Long targetMemberId) {
        MemberJpaEntity currentMember = memberUtil.getCurrentMember();

        if (currentMember.getId().equals(targetMemberId)) {
            throw new SelfBlockNotAllowedException();
        }

        if (memberBlockRepository.existsByBlockerIdAndBlockedId(currentMember.getId(), targetMemberId)) {
            throw new AlreadyBlockedException();
        }

        MemberJpaEntity targetMember = memberRepository.findById(targetMemberId)
                .orElseThrow(NotFoundMemberException::new);

        memberBlockRepository.save(MemberBlock.builder()
                .blocker(currentMember)
                .blocked(targetMember)
                .build());
    }
}
