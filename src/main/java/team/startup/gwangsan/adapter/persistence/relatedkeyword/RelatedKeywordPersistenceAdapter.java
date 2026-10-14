package team.startup.gwangsan.adapter.persistence.relatedkeyword;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;

import java.util.List;
import java.util.Optional;

/**
 * ponytail: JPA 모델은 아직 legacy(domain.relatedkeyword.entity/repository)다. auth/member(PR02)가 같은 Entity 를
 * 직접 쓰므로 Entity 이동은 해당 호출자가 전환된 뒤 수행한다(.omo/evidence/hex-pr03.md bridge 표).
 */
@Component
@RequiredArgsConstructor
public class RelatedKeywordPersistenceAdapter implements RelatedKeywordPort {

    private final RelatedKeywordRepository relatedKeywordRepository;
    private final MemberRelatedKeywordRepository memberRelatedKeywordRepository;

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
    public void bulkDeleteAllByMemberId(Long memberId) {
        memberRelatedKeywordRepository.bulkDeleteAllByMemberId(memberId);
    }

    private MemberKeyword toModel(MemberRelatedKeyword entity) {
        // LAZY member 프록시의 id 접근은 회원 행을 추가 조회하지 않는다.
        return new MemberKeyword(entity.getId(), entity.getMember().getId());
    }
}
