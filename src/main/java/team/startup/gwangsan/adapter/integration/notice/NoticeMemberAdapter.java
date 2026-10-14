package team.startup.gwangsan.adapter.integration.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;

import java.util.List;
import java.util.Optional;

// member 공개 MemberSummaryQuery(PR02)로 연결한다. 각 메서드 쿼리 1회.
@Component
@RequiredArgsConstructor
public class NoticeMemberAdapter implements NoticeMemberPort {

    private final MemberSummaryQuery memberSummaryQuery;

    // 회원 상세가 없으면 empty (기존 member_detail 조회와 같은 집합).
    @Override
    public Optional<Integer> findPlaceIdOf(Long memberId) {
        return memberSummaryQuery.findById(memberId)
                .filter(MemberSummary::hasDetail)
                .map(MemberSummary::placeId);
    }

    @Override
    public List<Long> findMemberIdsInPlace(Integer placeId) {
        return memberSummaryQuery.findMemberIdsInPlace(placeId);
    }
}
