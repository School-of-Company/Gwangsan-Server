package team.startup.gwangsan.application.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.admin.data.AdminAlertCommand;
import team.startup.gwangsan.application.admin.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.application.admin.port.out.AdminAlertPort;
import team.startup.gwangsan.application.admin.port.out.AdminMemberPort;
import team.startup.gwangsan.application.admin.port.out.AdminTradeCancelPort;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.model.AdminAlert;
import team.startup.gwangsan.domain.admin.policy.AdminPlaceScope;

@Service
@RequiredArgsConstructor
public class AdminApproveTradeCancelService implements ApproveTradeCancelUseCase {

    private final AdminAlertPort adminAlertPort;
    private final AdminScopeResolver adminScopeResolver;
    private final AdminMemberPort adminMemberPort;
    private final AdminTradeCancelPort adminTradeCancelPort;

    /**
     * 잠금 순서가 핵심이다. TradeCancel 을 잠금 전에 엔티티로 읽으면 그 값이 1차 캐시에 남아,
     * 양측 동의로 이미 철회된 뒤에도 PENDING 으로 보여 광산이 두 번 환불된다. 거래 업무의 승인 command 가
     * 상품 ID 스칼라 조회 → 상품 잠금 → TradeCancel 최초 로드 순서로 실행하고, 그 뒤 구매자·판매자 지점 검사를 콜백으로 부른다.
     * READ_COMMITTED 이유는 거래 업무 RequestTradeCancelService 주석 참고(승인 command 는 이 트랜잭션에 참여한다).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(AdminAlertCommand command) {
        AdminAlert alert = adminAlertPort.findWithRequester(command.alertId())
                .orElseThrow(NotFoundAdminAlertException::new);
        AdminPlaceScope scope = adminScopeResolver.resolve(command.actor());
        adminTradeCancelPort.approve(alert.sourceId(), (buyerId, sellerId) -> {
            Integer buyerPlaceId = adminMemberPort.loadPlace(buyerId).placeId();
            Integer sellerPlaceId = adminMemberPort.loadPlace(sellerId).placeId();
            scope.validate(buyerPlaceId);
            scope.validate(sellerPlaceId);
        });
    }
}
