package team.startup.gwangsan.adapter.persistence.relatedkeyword;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RelatedKeywordPersistenceAdapter implements RelatedKeywordPort {

    private final RelatedKeywordJpaRepository relatedKeywordRepository;
    private final MemberRelatedKeywordJpaRepository memberRelatedKeywordRepository;
    private final EntityManager entityManager;

    @Override
    public List<RelatedKeywordResult> findAll() {
        return relatedKeywordRepository.findAll().stream()
                .map(keyword -> new RelatedKeywordResult(keyword.getId(), keyword.getName()))
                .toList();
    }

    @Override
    public List<RelatedKeywordResult> findAllByMemberId(Long memberId) {
        return memberRelatedKeywordRepository.findAllByMemberId(memberId).stream()
                .map(mrk -> new RelatedKeywordResult(mrk.getRelatedKeyword().getId(), mrk.getRelatedKeyword().getName()))
                .toList();
    }

    @Override
    public Optional<MemberKeyword> findMemberKeyword(Long memberRelatedKeywordId) {
        return memberRelatedKeywordRepository.findById(memberRelatedKeywordId)
                .map(this::toModel);
    }

    @Override
    public void deleteMemberKeyword(Long memberRelatedKeywordId) {
        memberRelatedKeywordRepository.deleteById(memberRelatedKeywordId);
    }

    @Override
    public void addMemberKeywords(Long memberId, List<String> names) {
        MemberJpaEntity member = member(memberId);
        for (String name : names) {
            memberRelatedKeywordRepository.save(MemberRelatedKeywordJpaEntity.builder()
                    .member(member)
                    .relatedKeyword(findOrCreate(name))
                    .build());
        }
    }

    @Override
    public void replaceMemberKeywords(Long memberId, List<String> names) {
        MemberJpaEntity member = member(memberId);
        memberRelatedKeywordRepository.deleteAllByMember(member);
        memberRelatedKeywordRepository.saveAll(names.stream()
                .map(name -> new MemberRelatedKeywordJpaEntity(findOrCreate(name), member))
                .toList());
    }

    @Override
    public void bulkDeleteAllByMemberId(Long memberId) {
        memberRelatedKeywordRepository.bulkDeleteAllByMemberId(memberId);
    }

    private RelatedKeywordJpaEntity findOrCreate(String name) {
        return relatedKeywordRepository.findByName(name)
                .orElseGet(() -> relatedKeywordRepository.save(new RelatedKeywordJpaEntity(name)));
    }

    private MemberJpaEntity member(Long memberId) {
        return entityManager.getReference(MemberJpaEntity.class, memberId);
    }

    private MemberKeyword toModel(MemberRelatedKeywordJpaEntity entity) {
        // LAZY member 프록시의 id 접근은 회원 행을 추가 조회하지 않는다.
        return new MemberKeyword(entity.getId(), entity.getMember().getId());
    }
}
