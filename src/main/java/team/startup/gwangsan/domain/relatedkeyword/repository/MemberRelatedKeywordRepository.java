package team.startup.gwangsan.domain.relatedkeyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;

import java.util.List;

public interface MemberRelatedKeywordRepository extends JpaRepository<MemberRelatedKeyword, Long> {
    List<MemberRelatedKeyword> findAllByMember(Member member);
}
