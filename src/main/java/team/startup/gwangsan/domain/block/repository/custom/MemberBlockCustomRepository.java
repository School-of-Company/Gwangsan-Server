package team.startup.gwangsan.domain.block.repository.custom;

public interface MemberBlockCustomRepository {
    boolean existsBlockBetween(Long memberId1, Long memberId2);
}
