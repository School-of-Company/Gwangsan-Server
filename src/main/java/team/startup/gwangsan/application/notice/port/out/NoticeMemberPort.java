package team.startup.gwangsan.application.notice.port.out;

import java.util.List;
import java.util.Optional;

public interface NoticeMemberPort {

    // 회원의 소속 지점. 회원 상세가 없으면 empty.
    Optional<Integer> findPlaceIdOf(Long memberId);

    List<Long> findMemberIdsInPlace(Integer placeId);
}
