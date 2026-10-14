package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.AdminScope;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.port.in.FindAllUserInfoQuery;
import team.startup.gwangsan.application.member.port.out.MemberProfileQueryPort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotAllowedUserAccessException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAllUserInfoService implements FindAllUserInfoQuery {

    private final MemberProfileQueryPort memberProfileQueryPort;

    @Override
    @Transactional(readOnly = true)
    public List<MemberProfile> query(ActorContext actor, String nickname, String placeName) {
        // 기존 순서: 관리자 상세 조회(404) → 역할 검사(403)
        AdminScope scope = memberProfileQueryPort.findAdminScope(actor.memberId())
                .orElseThrow(NotFoundMemberException::new);
        Integer placeId = null;
        Integer headId = null;
        switch (actor.role()) {
            case ROLE_HEAD_ADMIN -> headId = scope.headId();
            case ROLE_PLACE_ADMIN -> placeId = scope.placeId();
            default -> throw new NotAllowedUserAccessException();
        }
        return memberProfileQueryPort.findAll(placeId, headId, nickname, placeName);
    }
}
