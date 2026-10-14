package team.startup.gwangsan.application.auth.data;

/** osType 은 기기 OS 이름(ANDROID/IOS) 또는 null 이다. */
public record SignInCommand(String nickname, String password, String deviceToken, String deviceId, String osType) {
}
