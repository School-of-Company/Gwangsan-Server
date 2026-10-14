package team.startup.gwangsan.adapter.integration.member;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;

import java.util.List;

// bridge: relatedkeyword 공개 command/query 가 생기면(PR03) 교체한다. 기존 저장 순서를 그대로 따른다.
@Component
@RequiredArgsConstructor
public class LegacyMemberKeywordAdapter implements MemberKeywordPort {

    private final RelatedKeywordRepository relatedKeywordRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    private final EntityManager entityManager;

    @Override
    public List<String> findNames(Long memberId) {
        return memberRelatedKeywordRepository.findAllByMember(member(memberId)).stream()
                .map(mrk -> mrk.getRelatedKeyword().getName())
                .toList();
    }

    @Override
    public void add(Long memberId, List<String> names) {
        MemberJpaEntity member = member(memberId);
        for (String name : names) {
            memberRelatedKeywordRepository.save(MemberRelatedKeyword.builder()
                    .member(member)
                    .relatedKeyword(findOrCreate(name))
                    .build());
        }
    }

    @Override
    public void replace(Long memberId, List<String> names) {
        MemberJpaEntity member = member(memberId);
        memberRelatedKeywordRepository.deleteAllByMember(member);
        memberRelatedKeywordRepository.saveAll(names.stream()
                .map(name -> new MemberRelatedKeyword(findOrCreate(name), member))
                .toList());
    }

    private RelatedKeyword findOrCreate(String name) {
        return relatedKeywordRepository.findByName(name)
                .orElseGet(() -> relatedKeywordRepository.save(new RelatedKeyword(name)));
    }

    private MemberJpaEntity member(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }
}
