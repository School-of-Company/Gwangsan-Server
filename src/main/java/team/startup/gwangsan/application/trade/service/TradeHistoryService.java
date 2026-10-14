package team.startup.gwangsan.application.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.trade.data.TradeHistoryCriteria;
import team.startup.gwangsan.application.trade.data.TradeHistoryItem;
import team.startup.gwangsan.application.trade.data.TradeHistoryRow;
import team.startup.gwangsan.application.trade.data.TradeImage;
import team.startup.gwangsan.application.trade.port.in.TradeHistoryQuery;
import team.startup.gwangsan.application.trade.port.out.TradeHistoryPort;
import team.startup.gwangsan.application.trade.port.out.TradeProductPort;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TradeHistoryService implements TradeHistoryQuery {

    private final TradeHistoryPort tradeHistoryPort;
    private final TradeProductPort tradeProductPort;

    @Override
    @Transactional(readOnly = true)
    public List<TradeHistoryItem> query(TradeHistoryCriteria criteria) {
        long actorId = criteria.actorId();
        List<TradeHistoryRow> rows = tradeHistoryPort.findByMemberAndStatus(actorId, criteria.status());
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = rows.stream().map(TradeHistoryRow::productId).distinct().toList();
        Map<Long, TradeImage> images = tradeProductPort.findRepresentativeImages(productIds);

        return rows.stream()
                .map(row -> {
                    boolean actorIsBuyer = row.buyerId() == actorId;
                    return new TradeHistoryItem(
                            row.tradeId(),
                            actorIsBuyer,
                            row.status(),
                            row.completedAt(),
                            actorIsBuyer ? row.sellerId() : row.buyerId(),
                            actorIsBuyer ? row.sellerNickname() : row.buyerNickname(),
                            row.productId(),
                            row.productTitle(),
                            row.gwangsan(),
                            images.get(row.productId()));
                })
                .toList();
    }
}
