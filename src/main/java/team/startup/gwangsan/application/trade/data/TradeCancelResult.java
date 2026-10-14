package team.startup.gwangsan.application.trade.data;

/** cancelled 가 true 면 상대방 요청에 대한 동의로 즉시 철회됐고, false 면 관리자 승인 대기로 접수됐다. */
public record TradeCancelResult(boolean cancelled) {
}
