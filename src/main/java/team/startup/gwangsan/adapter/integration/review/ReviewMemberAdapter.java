package team.startup.gwangsan.adapter.integration.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.port.in.AddMemberLightUseCase;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.application.review.port.out.ReviewMemberPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

/**
 * review → member 공개 Port 연결. 빛 가산(AddMemberLightUseCase)은 MANDATORY 라 후기 저장 트랜잭션 안에서만 실행되고,
 * 상세가 없으면 NotFoundMemberDetailException 으로 후기 저장까지 rollback 된다(기존 규칙: null 은 0, 0~100 clamp).
 */
@Component
@RequiredArgsConstructor
public class ReviewMemberAdapter implements ReviewMemberPort {

    private final MemberAccountQuery memberAccountQuery;
    private final AddMemberLightUseCase addMemberLightUseCase;

    @Override
    public String findNickname(long memberId) {
        return memberAccountQuery.findById(memberId).map(MemberAccount::nickname).orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public void addLight(long memberId, Integer light) {
        addMemberLightUseCase.execute(memberId, light);
    }
}
