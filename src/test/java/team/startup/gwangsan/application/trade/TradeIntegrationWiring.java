package team.startup.gwangsan.application.trade;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import team.startup.gwangsan.adapter.event.trade.TradeStatusEventPublisher;
import team.startup.gwangsan.adapter.integration.alert.LegacyAlertSourceAdapter;
import team.startup.gwangsan.adapter.integration.trade.*;
import team.startup.gwangsan.adapter.persistence.chat.ChatMessagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.chat.ChatRoomPersistenceAdapter;
import team.startup.gwangsan.application.chat.service.ChatRoomLookupService;
import team.startup.gwangsan.adapter.persistence.alert.AlertPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.MemberPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.notice.NoticePersistenceAdapter;
import team.startup.gwangsan.application.notice.service.NoticePublicQueryService;
import team.startup.gwangsan.adapter.persistence.post.ProductImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ProductPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.post.ReservationPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.trade.TradeCancelPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.trade.TradeCompletePersistenceAdapter;
import team.startup.gwangsan.application.alert.service.CreateAlertService;
import team.startup.gwangsan.application.member.service.ChangeGwangsanService;
import team.startup.gwangsan.application.member.service.MemberAccountService;
import team.startup.gwangsan.application.post.service.PendingReservationService;
import team.startup.gwangsan.application.post.service.ProductPublicQueryService;
import team.startup.gwangsan.application.post.service.ProductStatusService;
import team.startup.gwangsan.application.trade.service.*;
import team.startup.gwangsan.application.image.service.ImageQueryService;
import team.startup.gwangsan.adapter.persistence.image.ImagePersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.block.MemberBlockPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.report.ReportPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.suspend.SuspendPersistenceAdapter;
import team.startup.gwangsan.application.block.service.BlockQueryService;
import team.startup.gwangsan.application.report.service.ReportQueryService;
import team.startup.gwangsan.application.suspend.service.SuspensionQueryService;

/**
 * @DataJpaTest 에서 거래 쓰기 유스케이스를 실제 persistence adapter 와 선행 업무 공개 Port 구현
 * (post 상품·예약, member 잔액·계정, alert 원자 저장)으로 띄운다.
 */
@TestConfiguration
@Import({RequestTradeCompletionService.class, WithdrawTradeCompletionService.class, RequestTradeCancelService.class,
        WithdrawTradeCancelService.class, ApproveTradeCancelService.class, TradeCancelApplier.class, TradeStateService.class,
        TradeCancelQueryService.class, TradeCompletePersistenceAdapter.class, TradeCancelPersistenceAdapter.class,
        TradePostAdapter.class, TradeChatBridge.class, ChatRoomLookupService.class, ChatRoomPersistenceAdapter.class,
        ChatMessagePersistenceAdapter.class, TradeMemberAdapter.class, TradeBlockBridge.class,
        TradeAlertBridge.class, TradePushAdapter.class, TradeStatusEventPublisher.class,
        TradeImageAdapter.class, ImageQueryService.class, ImagePersistenceAdapter.class,
        ProductStatusService.class, ProductPublicQueryService.class, PendingReservationService.class,
        ProductPersistenceAdapter.class, ProductImagePersistenceAdapter.class, ReservationPersistenceAdapter.class,
        ChangeGwangsanService.class, MemberAccountService.class, MemberPersistenceAdapter.class,
        CreateAlertService.class, AlertPersistenceAdapter.class, LegacyAlertSourceAdapter.class,
        NoticePublicQueryService.class, NoticePersistenceAdapter.class,
        BlockQueryService.class, MemberBlockPersistenceAdapter.class,
        ReportQueryService.class, ReportPersistenceAdapter.class, SuspensionQueryService.class, SuspendPersistenceAdapter.class,
        team.startup.gwangsan.application.admin.service.AdminAlertSupportService.class,
        team.startup.gwangsan.adapter.persistence.admin.AdminAlertPersistenceAdapter.class})
public class TradeIntegrationWiring {
}
