package team.startup.gwangsan.application.auth.data;

public record ResetPasswordCommand(String phoneNumber, String newPassword) {
}
