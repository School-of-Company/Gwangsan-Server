package team.startup.gwangsan.adapter.integration.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.data.AuthorProfile;
import team.startup.gwangsan.application.post.port.out.BlockPolicyPort;
import team.startup.gwangsan.application.post.port.out.ProductMemberPort;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.global.util.BlockValidator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * member·block 연결. 닉네임은 member 공개 {@link MemberAccountQuery} 를 쓴다.
 * bridge: 지점 ID·작성자 프로필(지점명·빛)은 member 공개 query 가 없어 MemberDetailJpaRepository 를 직접 쓰고(PR11/PR12 에서 공개 query 로 교체),
 * 차단 검사는 BlockValidator(PR09) 를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PostMemberBridgeAdapter implements ProductMemberPort, BlockPolicyPort {

    private final MemberAccountQuery memberAccountQuery;
    private final MemberDetailJpaRepository memberDetailRepository;
    private final BlockValidator blockValidator;

    @Override
    public Integer placeIdOf(Long memberId) {
        return memberDetailRepository.findPlaceByMemberId(memberId).getId();
    }

    @Override
    public AuthorProfile profile(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .map(PostMemberBridgeAdapter::toProfile)
                .orElseThrow(NotFoundMemberDetailException::new);
    }

    @Override
    public Map<Long, AuthorProfile> profiles(Collection<Long> memberIds) {
        return memberDetailRepository.findAllByMemberIdIn(List.copyOf(memberIds)).stream()
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
        blockValidator.validate(actorId, targetId);
    }

    private static AuthorProfile toProfile(MemberDetailJpaEntity detail) {
        return new AuthorProfile(detail.getMember().getId(), detail.getMember().getNickname(),
                detail.getPlace().getName(), detail.getLight());
    }
}
