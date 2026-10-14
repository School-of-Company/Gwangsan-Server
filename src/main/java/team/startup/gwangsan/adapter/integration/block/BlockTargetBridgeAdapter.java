package team.startup.gwangsan.adapter.integration.block;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.block.port.out.LoadBlockTargetPort;
import team.startup.gwangsan.application.member.port.in.MemberAccountQuery;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

/** block → member(PR02) 공개 query 연결. */
@Component
@RequiredArgsConstructor
public class BlockTargetBridgeAdapter implements LoadBlockTargetPort {

    private final MemberAccountQuery memberAccountQuery;

    @Override
    public void checkMemberExists(Long memberId) {
        memberAccountQuery.findById(memberId).orElseThrow(NotFoundMemberException::new);
    }
}
