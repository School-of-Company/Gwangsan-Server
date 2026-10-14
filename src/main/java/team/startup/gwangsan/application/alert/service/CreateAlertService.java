package team.startup.gwangsan.application.alert.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.alert.data.CreateAlertForMembersCommand;
import team.startup.gwangsan.application.alert.data.CreateAlertFromSourceCommand;
import team.startup.gwangsan.application.alert.data.RecordAlertCommand;
import team.startup.gwangsan.application.alert.port.in.CreateAlertFromSourceUseCase;
import team.startup.gwangsan.application.alert.port.in.RecordAlertUseCase;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReportSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.ReviewSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCancelSource;
import team.startup.gwangsan.application.alert.port.out.LoadAlertSourcePort.TradeCompleteSource;
import team.startup.gwangsan.application.alert.port.out.SaveAlertPort;
import team.startup.gwangsan.domain.alert.exception.UnsupportedAlertTypeException;
import team.startup.gwangsan.domain.alert.model.Alert;
import team.startup.gwangsan.domain.alert.model.AlertType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateAlertService implements RecordAlertUseCase, CreateAlertFromSourceUseCase {

    private final SaveAlertPort saveAlertPort;
    private final LoadAlertSourcePort loadAlertSourcePort;

    // REQUIRED: 호출한 업무 트랜잭션에 참여한다. REQUIRES_NEW 로 바꾸면 업무와 알림의 원자성이 깨진다.
    @Override
    @Transactional
    public Long record(RecordAlertCommand command) {
        if (!command.alert().type().isRecordedWithBusinessTransaction()) {
            throw new UnsupportedAlertTypeException();
        }
        return saveAlertPort.save(command.alert(), command.recipientMemberIds());
    }

    @Override
    @Transactional
    public void create(CreateAlertFromSourceCommand command) {
        Long sourceId = command.sourceId();
        Long memberId = command.memberId();
        loadAlertSourcePort.requireMember(memberId);

        switch (command.type()) {
            case TRADE_COMPLETE -> {
                TradeCompleteSource trade = loadAlertSourcePort.tradeComplete(sourceId);
                saveAlertPort.save(Alert.tradeCompleteForBuyer(trade.productId(), trade.sellerId(), trade.productTitle()),
                        List.of(trade.buyerId()));
                saveAlertPort.save(Alert.tradeCompleteForSeller(trade.productId(), trade.buyerId(), trade.productTitle()),
                        List.of(trade.sellerId()));
            }
            case TRADE_COMPLETE_REJECT -> saveAlertPort.save(
                    Alert.tradeCompleteReject(sourceId, loadAlertSourcePort.activeProductTitle(sourceId)), List.of(memberId));
            case OTHER_MEMBER_TRADE_COMPLETE -> {
                TradeCompleteSource trade = loadAlertSourcePort.tradeComplete(sourceId);
                saveAlertPort.save(Alert.otherMemberTradeComplete(trade.productId(), trade.sellerId(), trade.productTitle(),
                        trade.buyerNickname()), List.of(memberId));
            }
            case RECOMMENDER -> {
                loadAlertSourcePort.requireMember(sourceId);
                saveAlertPort.save(Alert.recommender(sourceId, loadAlertSourcePort.recommenderReward()), List.of(memberId));
            }
            case REVIEW -> {
                ReviewSource review = loadAlertSourcePort.review(sourceId);
                saveAlertPort.save(Alert.review(sourceId, review.reviewerId(), review.reviewerNickname()), List.of(memberId));
            }
            case REPORT_REJECT -> {
                ReportSource report = loadAlertSourcePort.report(sourceId);
                saveAlertPort.save(Alert.reportReject(sourceId, report.reporterId(), report.reportedNickname()), List.of(memberId));
            }
            case TRADE_CANCEL_REJECT -> saveAlertPort.save(
                    Alert.tradeCancelReject(sourceId, loadAlertSourcePort.tradeCancelRequester(sourceId)), List.of(memberId));
            case TRADE_CANCEL -> {
                // memberId 는 존재 검증용이다. 수신자는 거래 양측이다.
                TradeCancelSource cancel = loadAlertSourcePort.tradeCancelWithParties(sourceId);
                saveAlertPort.save(Alert.tradeCancel(sourceId, cancel.requesterId()), List.of(cancel.buyerId(), cancel.sellerId()));
            }
            case REPORT -> {
                // 기존 동작 그대로 sourceId 로 신고를 조회한다.
                ReportSource report = loadAlertSourcePort.report(sourceId);
                int days = loadAlertSourcePort.suspendedDays(command.suspendId());
                saveAlertPort.save(Alert.report(sourceId, report.reporterId(), days), List.of(memberId));
            }
            default -> throw new UnsupportedAlertTypeException();
        }
    }

    @Override
    @Transactional
    public void createForMembers(CreateAlertForMembersCommand command) {
        List<Long> memberIds = loadAlertSourcePort.existingMemberIds(command.memberIds());
        if (memberIds.isEmpty() || command.type() != AlertType.NOTICE) {
            return;
        }
        Long noticeId = command.sourceId();
        saveAlertPort.save(Alert.notice(noticeId, loadAlertSourcePort.noticeTitle(noticeId)), memberIds);
    }
}
