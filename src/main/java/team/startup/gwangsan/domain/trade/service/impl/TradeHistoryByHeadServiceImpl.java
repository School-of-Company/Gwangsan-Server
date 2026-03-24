package team.startup.gwangsan.domain.trade.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.presentation.dto.PlaceDto;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.domain.trade.repository.TradeCompleteRepository;
import team.startup.gwangsan.domain.trade.presentation.dto.request.constant.Period;
import team.startup.gwangsan.domain.trade.presentation.dto.response.HeadTradeHistoryResponse;
import team.startup.gwangsan.domain.trade.service.TradeHistoryByHeadService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeHistoryByHeadServiceImpl implements TradeHistoryByHeadService {

    private final TradeCompleteRepository tradeCompleteRepository;
    private final PlaceRepository placeRepository;

    @Override
    public List<HeadTradeHistoryResponse> execute(Period period, Integer headId) {
        int periodValue = period.getValue();
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, Long> history = tradeCompleteRepository.countByHeadId(periodValue, now, headId);

        List<Place> places = placeRepository.findAllById(history.keySet());
        Map<Integer, PlaceDto> placeMap = places.stream()
                .collect(Collectors.toMap(
                        p -> p.getId().intValue(),
                        p -> new PlaceDto(p.getId(), p.getName())
                ));

        return history.entrySet().stream()
                .map(e -> new HeadTradeHistoryResponse(
                        placeMap.get(e.getKey()),
                        e.getValue()
                ))
                .toList();
    }
}
