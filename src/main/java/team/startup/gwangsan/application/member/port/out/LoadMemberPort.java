package team.startup.gwangsan.application.member.port.out;

import team.startup.gwangsan.application.member.data.MemberSummary;
import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadMemberPort {

    Optional<Member> findById(Long memberId);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    Optional<MemberDetail> findDetail(Long memberId);

    boolean isBannedPhoneNumber(String phoneNumber);

    List<Long> findExistingIds(Collection<Long> memberIds);

    /** 회원 기준 left join 1 쿼리. */
    List<MemberSummary> findSummaries(Collection<Long> memberIds);

    List<Long> findMemberIdsInPlace(Integer placeId);
}
