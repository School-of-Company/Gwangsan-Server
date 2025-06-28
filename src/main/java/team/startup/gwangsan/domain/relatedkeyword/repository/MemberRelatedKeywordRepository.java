package team.startup.gwangsan.domain.relatedkeyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;

public interface MemberRelatedKeywordRepository extends JpaRepository<MemberRelatedKeyword, Long> {
}
