package team.startup.gwangsan.domain.relatedkeyword.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;

import java.util.Optional;

public interface RelatedKeywordRepository extends JpaRepository<RelatedKeyword, Long> {
    Optional<RelatedKeyword> findByName(String name);
}
