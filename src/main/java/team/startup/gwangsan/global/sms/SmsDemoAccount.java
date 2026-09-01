package team.startup.gwangsan.global.sms;

/**
 * App Store 심사(Review Notes)에 안내된 데모 번호/고정 코드.
 * 실제 수신이 불가능한 번호이므로 발송·검증 단계 모두 우회한다.
 */
public final class SmsDemoAccount {

    public static final String PHONE_NUMBER = "01011111111";
    public static final String FIXED_CODE = "000000";

    private SmsDemoAccount() {
    }

    public static boolean isDemo(String phoneNumber) {
        return PHONE_NUMBER.equals(phoneNumber);
    }
}
