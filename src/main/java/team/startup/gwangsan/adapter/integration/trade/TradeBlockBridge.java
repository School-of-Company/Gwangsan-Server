package team.startup.gwangsan.adapter.integration.trade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.trade.port.out.TradeBlockPort;
import team.startup.gwangsan.global.util.BlockValidator;

/** legacy bridge: block 공개 query(PR09) 전까지 BlockValidator 로 연결한다. */
@Component
@RequiredArgsConstructor
public class TradeBlockBridge implements TradeBlockPort {

    private final BlockValidator blockValidator;

    @Override
    public void validateNotBlocked(long memberId, long otherMemberId) {
        blockValidator.validate(memberId, otherMemberId);
    }
}
