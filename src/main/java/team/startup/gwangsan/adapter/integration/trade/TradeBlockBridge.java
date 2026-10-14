package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.port.out.TradeBlockPort;
import team.startup.gwangsan.application.block.port.in.BlockRelationQuery;

/** block 공개 query(PR09) 연결. 차단이면 BlockedMemberException(403). */
@Component
@RequiredArgsConstructor
public class TradeBlockBridge implements TradeBlockPort {

    private final BlockRelationQuery blockRelationQuery;

    @Override
    public void validateNotBlocked(long memberId, long otherMemberId) {
        blockRelationQuery.validateNotBlocked(memberId, otherMemberId);
    }
}
