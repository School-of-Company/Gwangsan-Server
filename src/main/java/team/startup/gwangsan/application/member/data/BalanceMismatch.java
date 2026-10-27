package team.startup.gwangsan.application.member.data;

/** 잔액과 원장 합이 다른 회원. */
public record BalanceMismatch(long memberId, int balance, long ledgerSum) {
}
