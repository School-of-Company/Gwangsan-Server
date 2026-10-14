package team.startup.gwangsan.application.member.port.in;

import team.startup.gwangsan.application.member.data.MemberSummary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 회원 요약 공개 조회. 각 메서드는 쿼리 1회이며 결과 순서는 보장하지 않는다. 상태 필터는 없다. */
public interface MemberSummaryQuery {

    Optional<MemberSummary> findById(Long memberId);

    /** 빈 입력이면 쿼리 없이 빈 목록. */
    List<MemberSummary> findAllByIds(Collection<Long> memberIds);

    /** 지점에 속한 회원(상세 기준) ID. */
    List<Long> findMemberIdsInPlace(Integer placeId);
}
