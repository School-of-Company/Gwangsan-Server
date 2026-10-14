package team.startup.gwangsan.adapter.persistence.block;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.block.data.BlockedMember;
import team.startup.gwangsan.application.block.port.out.LoadMemberBlockPort;
import team.startup.gwangsan.application.block.port.out.SaveMemberBlockPort;
import team.startup.gwangsan.domain.block.model.MemberBlock;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberBlockPersistenceAdapter implements LoadMemberBlockPort, SaveMemberBlockPort {

    private final MemberBlockJpaRepository memberBlockJpaRepository;
    private final EntityManager entityManager;

    @Override
    public boolean exists(Long blockerId, Long blockedId) {
        return memberBlockJpaRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public boolean existsBetween(Long memberId, Long otherMemberId) {
        return memberBlockJpaRepository.existsBlockBetween(memberId, otherMemberId);
    }

    @Override
    public List<BlockedMember> findBlockedMembers(Long blockerId) {
        return memberBlockJpaRepository.findAllByBlockerId(blockerId).stream()
                .map(block -> new BlockedMember(block.getBlocked().getId(), block.getBlocked().getNickname()))
                .toList();
    }

    // 회원 association 은 기존 FK 매핑 유지를 위한 저장 기술 예외다. 존재 확인을 마친 ID 의 reference 로 채운다.
    @Override
    public void save(MemberBlock memberBlock) {
        memberBlockJpaRepository.save(MemberBlockJpaEntity.builder()
                .blocker(entityManager.getReference(MemberJpaEntity.class, memberBlock.blockerId()))
                .blocked(entityManager.getReference(MemberJpaEntity.class, memberBlock.blockedId()))
                .build());
    }

    @Override
    public boolean delete(Long blockerId, Long blockedId) {
        return memberBlockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .map(block -> {
                    memberBlockJpaRepository.delete(block);
                    return true;
                })
                .orElse(false);
    }
}
