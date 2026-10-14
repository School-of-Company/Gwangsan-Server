package team.startup.gwangsan.adapter.web.trade.dto;

/** 본부 통계의 지점. 기존 PlaceDto 와 같은 JSON(id, name). */
public record TradePlaceResponse(
        Integer id,
        String name
) {
}
