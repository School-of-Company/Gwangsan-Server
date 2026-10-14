package team.startup.gwangsan.adapter.persistence.relatedkeyword;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RelatedKeywordJpaRepository extends JpaRepository<RelatedKeywordJpaEntity, Long> {
    Optional<RelatedKeywordJpaEntity> findByName(String name);
}
