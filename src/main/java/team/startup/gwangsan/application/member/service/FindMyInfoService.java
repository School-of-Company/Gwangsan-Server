package team.startup.gwangsan.application.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.data.MyInfoResult;
import team.startup.gwangsan.application.member.port.in.FindMyInfoQuery;
import team.startup.gwangsan.application.member.port.out.MemberKeywordPort;
import team.startup.gwangsan.application.member.port.out.MemberProfileQueryPort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

@Service
@RequiredArgsConstructor
public class FindMyInfoService implements FindMyInfoQuery {

    private final MemberProfileQueryPort memberProfileQueryPort;
    private final MemberKeywordPort memberKeywordPort;

    @Override
    @Transactional(readOnly = true)
    public MyInfoResult query(ActorContext actor) {
        MemberProfile profile = memberProfileQueryPort.findMyProfile(actor.memberId())
                .orElseThrow(NotFoundMemberException::new);
        return new MyInfoResult(profile.memberId(), profile.nickname(), profile.placeName(), profile.headName(),
                profile.dongName(), profile.light(), profile.gwangsan(), profile.description(),
                memberKeywordPort.findNames(profile.memberId()));
    }
}
