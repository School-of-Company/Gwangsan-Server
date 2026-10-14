package team.startup.gwangsan.domain.admin.policy;

public final class AdminAlertTitle {

    public static final String SIGN_UP = "새로운 회원가입 요청";
    public static final String TRADE_CANCEL = "거래 철회 요청";

    // report 의 ReportType 과 같은 이름을 유지한다.
    public enum ReportCategory {
        SEXUAL,
        ABUSE_HATE_HARASSMENT,
        SPAM_AD,
        IMPERSONATION,
        SELF_HARM_DANGER,
        ETC
    }

    private AdminAlertTitle() {
    }

    public static String report(ReportCategory category) {
        return switch (category) {
            case SEXUAL -> "음란/성적 콘텐츠 신고";
            case ABUSE_HATE_HARASSMENT -> "욕설/혐오/괴롭힘 신고";
            case SPAM_AD -> "스팸/광고 신고";
            case IMPERSONATION -> "사칭 신고";
            case SELF_HARM_DANGER -> "자해/위험 신고";
            case ETC -> "기타 신고";
        };
    }
}
