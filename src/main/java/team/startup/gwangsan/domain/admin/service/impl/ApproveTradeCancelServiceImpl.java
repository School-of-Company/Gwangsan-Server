package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.ApproveTradeCancelService;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.trade.entity.TradeCancel;
import team.startup.gwangsan.domain.trade.entity.TradeComplete;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.domain.trade.repository.TradeCancelRepository;
import team.startup.gwangsan.domain.trade.service.TradeCancelApplier;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class ApproveTradeCancelServiceImpl implements ApproveTradeCancelService {

    private final AdminAlertRepository adminAlertRepository;
    private final TradeCancelRepository tradeCancelRepository;
    private final ProductRepository productRepository;
    private final MemberUtil memberUtil;
    private final MemberDetailRepository memberDetailRepository;
    private final ValidatePlaceUtil validatePlaceUtil;
    private final TradeCancelApplier tradeCancelApplier;

    /**
     * 잠금 순서가 핵심이다. TradeCancel 을 잠금 전에 엔티티로 읽으면 그 값이 1차 캐시에 남아,
     * 양측 동의로 이미 철회된 뒤에도 PENDING 으로 보여 광산이 두 번 환불된다. 그래서 상품 ID 만
     * 스칼라로 먼저 읽어 잠금을 잡고, TradeCancel 은 그 뒤에 처음 로드한다.
     * READ_COMMITTED 이유는 {@code TradeCancelServiceImpl} 주석 참고.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void execute(Long alertId) {
        Member admin = memberUtil.getCurrentMember();
        AdminAlert alert = adminAlertRepository.findByIdWithMember(alertId)
                .orElseThrow(NotFoundAdminAlertException::new);

        MemberDetail adminDetail = getMemberDetail(admin.getId());

        Long productId = tradeCancelRepository.findProductIdById(alert.getSourceId())
                .orElseThrow(NotFoundTradeCancelException::new);

        productRepository.findByIdForUpdate(productId)
                .orElseThrow(NotFoundProductException::new);

        TradeCancel tradeCancel = tradeCancelRepository.findByIdWithTradeCompleteAndBuyerAndSellerAndProduct(alert.getSourceId())
                .orElseThrow(NotFoundTradeCancelException::new);

        TradeComplete tradeComplete = tradeCancel.getTradeComplete();

        MemberDetail buyerDetail = getMemberDetail(tradeComplete.getBuyer().getId());
        MemberDetail sellerDetail = getMemberDetail(tradeComplete.getSeller().getId());

        validatePlaceUtil.validateSamePlace(admin, adminDetail, buyerDetail);
        validatePlaceUtil.validateSamePlace(admin, adminDetail, sellerDetail);

        tradeCancelApplier.apply(tradeCancel);
    }

    private MemberDetail getMemberDetail(Long memberId) {
        return memberDetailRepository.findById(memberId)
                .orElseThrow(NotFoundMemberDetailException::new);
    }
}
