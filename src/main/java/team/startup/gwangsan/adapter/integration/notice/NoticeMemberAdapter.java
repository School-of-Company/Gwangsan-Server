package team.startup.gwangsan.adapter.integration.notice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.application.notice.port.out.NoticeMemberPort;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.List;
import java.util.Optional;

// bridge: member 공개 Port 에 "회원 소속 지점·지점 회원 id" 조회가 없어 member persistence 를 직접 쓴다.
// member 공개 query 가 생기면 교체한다(최종 확인 PR12).
@Component
@RequiredArgsConstructor
public class NoticeMemberAdapter implements NoticeMemberPort {

    private final MemberDetailJpaRepository memberDetailRepository;
    private final PlaceRepository placeRepository;

    // MemberDetail 의 id 는 member_id(@MapsId)다.
    @Override
    public Optional<Integer> findPlaceIdOf(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .map(detail -> detail.getPlace().getId());
    }

    @Override
    public List<Long> findMemberIdsInPlace(Integer placeId) {
        return memberDetailRepository.findAllByPlace(placeRepository.getReferenceById(placeId)).stream()
                .map(MemberDetailJpaEntity::getId)
                .toList();
    }
}
