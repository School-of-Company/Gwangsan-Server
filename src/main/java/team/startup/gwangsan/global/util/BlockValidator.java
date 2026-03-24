package team.startup.gwangsan.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.member.entity.Member;

@Component
@RequiredArgsConstructor
public class BlockValidator {

    private final MemberBlockRepository memberBlockRepository;

    public void validate(Long currentMemberId, Long targetMemberId) {
        if (memberBlockRepository.existsBlockBetween(currentMemberId, targetMemberId)) {
            throw new BlockedMemberException();
        }
    }

    public void validate(Member currentMember, Member targetMember) {
        validate(currentMember.getId(), targetMember.getId());
    }
}
