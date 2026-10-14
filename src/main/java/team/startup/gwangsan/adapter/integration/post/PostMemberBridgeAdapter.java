package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.AuthorProfile;
import team.startup.gwangsan.application.post.port.out.BlockPolicyPort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.application.block.port.in.BlockRelationQuery;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * member·block 연결. 닉네임은 {@link MemberAccountQuery}, 지점 ID·작성자 프로필은 {@link MemberSummaryQuery} 를 쓴다.
 * 요약은 회원 기준 left join 이라 상세가 없는 회원은 hasDetail 로 걸러 기존 상세 기준 조회와 같은 결과를 낸다.
 * 차단 검사는 block 공개 BlockRelationQuery(PR09) 를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PostMemberBridgeAdapter implements ProductMemberPort, BlockPolicyPort {

    private final MemberAccountQuery memberAccountQuery;
    private final MemberSummaryQuery memberSummaryQuery;
    private final BlockRelationQuery blockRelationQuery;

    @Override
    public Integer placeIdOf(Long memberId) {
        return detailOf(memberId).orElseThrow(NotFoundMemberException::new).placeId();
    }

    @Override
    public AuthorProfile profile(Long memberId) {
        return detailOf(memberId)
                .map(PostMemberBridgeAdapter::toProfile)
                .orElseThrow(NotFoundMemberDetailException::new);
    }

    @Override
    public Map<Long, AuthorProfile> profiles(Collection<Long> memberIds) {
        return memberSummaryQuery.findAllByIds(memberIds).stream()
                .filter(MemberSummary::hasDetail)
                .map(PostMemberBridgeAdapter::toProfile)
                .collect(Collectors.toMap(AuthorProfile::memberId, profile -> profile));
    }

    @Override
    public String nicknameOf(Long memberId) {
        return memberAccountQuery.findById(memberId)
                .orElseThrow(NotFoundMemberException::new)
                .nickname();
    }

    @Override
    public void validateNotBlocked(Long actorId, Long targetId) {
        blockRelationQuery.validateNotBlocked(actorId, targetId);
    }

    private Optional<MemberSummary> detailOf(Long memberId) {
        return memberSummaryQuery.findById(memberId).filter(MemberSummary::hasDetail);
    }

    private static AuthorProfile toProfile(MemberSummary summary) {
        return new AuthorProfile(summary.memberId(), summary.nickname(), summary.placeName(), summary.light());
    }
}
