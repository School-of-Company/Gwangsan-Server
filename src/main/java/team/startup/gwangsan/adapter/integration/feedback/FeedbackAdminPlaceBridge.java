package team.startup.gwangsan.adapter.integration.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.feedback.port.out.AdminPlaceLookupPort;
import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.application.member.port.in.MemberSummaryQuery;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;

/** 문의 관리자 권한 범위 계산에 쓰는 지점 조회. member·place 공개 query 를 쓴다. */
@Component
@RequiredArgsConstructor
public class FeedbackAdminPlaceBridge implements AdminPlaceLookupPort {

    private final MemberSummaryQuery memberSummaryQuery;
    private final PlaceQuery placeQuery;

    @Override
    public Integer ownPlaceId(Long memberId) {
        return memberSummaryQuery.findById(memberId)
                .filter(MemberSummary::hasDetail)
                .map(MemberSummary::placeId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }

    @Override
    public Integer requirePlace(Integer placeId) {
        return placeQuery.findById(placeId).orElseThrow(PlaceNotFoundException::new).id();
    }
}
