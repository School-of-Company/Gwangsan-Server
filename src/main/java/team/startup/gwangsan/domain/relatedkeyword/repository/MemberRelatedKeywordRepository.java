package team.startup.gwangsan.domain.relatedkeyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;

import java.util.List;

public interface MemberRelatedKeywordRepository extends JpaRepository<MemberRelatedKeyword, Long> {
    List<MemberRelatedKeyword> findAllByMember(MemberJpaEntity member);

    void deleteAllByMember(MemberJpaEntity member);
}
