package team.startup.gwangsan.domain.admin.policy;

import java.util.Optional;

public final class SignUpReward {

    public static final int SIGNUP_GWANGSAN_REWARD = 10000;
    public static final int RECOMMENDER_GWANGSAN_REWARD = 5000;

    private SignUpReward() {
    }

    /** 재가입자는 탈퇴 때 남긴 광산을 복원하고, 신규 가입자는 가입 보상을 받는다. */
    public static int gwangsanFor(Optional<Integer> withdrawnGwangsan) {
        return withdrawnGwangsan.orElse(SIGNUP_GWANGSAN_REWARD);
    }
}
