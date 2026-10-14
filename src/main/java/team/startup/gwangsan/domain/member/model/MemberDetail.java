package team.startup.gwangsan.domain.member.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberDetail {

    private static final int INITIAL_GWANGSAN = 0;
    private static final int INITIAL_LIGHT = 1;
    private static final int MAX_LIGHT = 100;

    private final Long memberId;
    private final Integer dongId;
    private Integer placeId;
    private Integer gwangsan;
    private Integer light;
    private String description;

    public static MemberDetail applicant(Integer dongId, Integer placeId, String description) {
        return new MemberDetail(null, dongId, placeId, INITIAL_GWANGSAN, INITIAL_LIGHT, description);
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void changePlace(Integer placeId) {
        this.placeId = placeId;
    }

    // 음수 잔액 허용은 기존 정책이다. 거래 경로에서 바꾸지 않는다.
    public void plusGwangsan(int amount) {
        this.gwangsan = this.gwangsan + amount;
    }

    public void minusGwangsan(int amount) {
        this.gwangsan = this.gwangsan - amount;
    }

    /** 관리자 조정은 결과가 0 미만이면 0 으로 맞춘다. */
    public void adjustGwangsan(int delta) {
        this.gwangsan = Math.max(0, this.gwangsan + delta);
    }

    public void plusLight(Integer delta) {
        int current = light != null ? light : 0;
        this.light = Math.min(MAX_LIGHT, Math.max(0, current + (delta != null ? delta : 0)));
    }
}
