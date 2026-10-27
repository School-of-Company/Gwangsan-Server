package team.startup.gwangsan.domain.member.model;

/**
 * 광산 원장 사유. ref_id 와 함께 (type, ref_id, member_id) 유니크 키를 이뤄 같은 사유의 이중 반영을 막는다.
 * 재가입 복원액은 SignUpReward.gwangsanFor 가 가입 보상과 합쳐 한 금액으로 주므로 SIGNUP_REWARD 하나로 기록한다.
 */
public enum GwangsanLedgerType {
    TRADE_COMPLETE,
    TRADE_CANCEL,
    SIGNUP_REWARD,
    RECOMMENDER_REWARD,
    ADMIN_ADJUST,
    OPENING_BALANCE
}
