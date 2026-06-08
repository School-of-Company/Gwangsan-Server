package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.exception.InvalidTradeStatisticsPeriodException;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.PlaceTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByPlaceService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradeHistoryByPlaceServiceImpl implements TradeHistoryByPlaceService {

    private final TradeCompleteRepository tradeCompleteRepository;

    @Override
    public PlaceTradeHistoryResponse execute(Period period, Integer placeId) {
        return new PlaceTradeHistoryResponse(
                tradeCompleteRepository.countByPlaceId(
                        period.getValue(), LocalDateTime.now(), placeId)
        );
    }

    @Override
    public PlaceTradeHistoryResponse execute(Integer placeId, LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        return new PlaceTradeHistoryResponse(
                tradeCompleteRepository.countByPlaceIdBetween(
                        placeId,
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                )
        );
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidTradeStatisticsPeriodException();
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidTradeStatisticsPeriodException();
        }
    }
}
