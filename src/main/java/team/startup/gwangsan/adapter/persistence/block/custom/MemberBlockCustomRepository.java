package team.startup.gwangsan.adapter.persistence.block.custom;

public interface MemberBlockCustomRepository {
    boolean existsBlockBetween(Long memberId1, Long memberId2);
}
