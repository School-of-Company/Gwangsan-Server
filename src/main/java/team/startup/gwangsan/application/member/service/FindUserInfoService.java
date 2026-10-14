package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.data.UserInfoResult;
import team.startup.gwangsan.application.member.port.in.FindUserInfoQuery;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.application.member.port.out.MemberProfileQueryPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

@Service
@RequiredArgsConstructor
public class FindUserInfoService implements FindUserInfoQuery {

    private final MemberProfileQueryPort memberProfileQueryPort;
    private final MemberKeywordPort memberKeywordPort;

    @Override
    @Transactional(readOnly = true)
    public UserInfoResult query(Long memberId) {
        MemberProfile profile = memberProfileQueryPort.findUserProfile(memberId)
                .orElseThrow(NotFoundMemberException::new);
        return new UserInfoResult(profile.memberId(), profile.nickname(), profile.placeName(), profile.light(),
                profile.gwangsan(), profile.description(), memberKeywordPort.findNames(profile.memberId()),
                profile.name(), profile.phoneNumber(), profile.role(), profile.status(), profile.joinedAt());
    }
}
