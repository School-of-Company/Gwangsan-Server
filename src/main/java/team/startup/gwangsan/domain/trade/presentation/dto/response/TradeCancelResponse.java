package team.startup.gwangsan.domain.trade.presentation.dto.response;

/**
 * 철회 요청의 처리 결과.
 *
 * @param cancelled true 면 상대방이 이미 요청해 둔 상태라 즉시 철회까지 끝났고,
 *                  false 면 관리자 승인을 기다리는 요청만 접수됐다.
 *                  응답 코드는 두 경우 모두 200 이므로 이 값으로만 구분할 수 있다.
 */
public record TradeCancelResponse(
        boolean cancelled
) {
}
