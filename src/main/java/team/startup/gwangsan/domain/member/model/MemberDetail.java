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

    public void plusLight(Integer delta) {
        int current = light != null ? light : 0;
        this.light = Math.min(MAX_LIGHT, Math.max(0, current + (delta != null ? delta : 0)));
    }
}
