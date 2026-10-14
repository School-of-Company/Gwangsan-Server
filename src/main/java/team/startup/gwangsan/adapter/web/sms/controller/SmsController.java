package team.startup.gwangsan.adapter.web.sms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.sms.dto.SendSmsRequest;
import team.startup.gwangsan.adapter.web.sms.dto.VerifyCodeRequest;
import team.startup.gwangsan.application.sms.port.in.SendFindNicknameSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.SendResetPasswordSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.SendSmsUseCase;
import team.startup.gwangsan.application.sms.port.in.VerifyCodeUseCase;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SendSmsUseCase sendSmsUseCase;
    private final SendResetPasswordSmsUseCase sendResetPasswordSmsUseCase;
    private final SendFindNicknameSmsUseCase sendFindNicknameSmsUseCase;
    private final VerifyCodeUseCase verifyCodeUseCase;

    @PostMapping
    public ResponseEntity<Void> sendSms(@RequestBody @Valid SendSmsRequest request) {
        sendSmsUseCase.execute(request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifySms(@RequestBody @Valid VerifyCodeRequest request) {
        return verify(request);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> sendResetPasswordSms(@RequestBody @Valid SendSmsRequest request) {
        sendResetPasswordSmsUseCase.execute(request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/verify")
    public ResponseEntity<Void> verifyResetPasswordSms(@RequestBody @Valid VerifyCodeRequest request) {
        return verify(request);
    }

    @PostMapping("/nickname")
    public ResponseEntity<Void> sendFindNicknameSms(@RequestBody @Valid SendSmsRequest request) {
        sendFindNicknameSmsUseCase.execute(request.phoneNumber());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/nickname/verify")
    public ResponseEntity<Void> verifyFindNicknameSms(@RequestBody @Valid VerifyCodeRequest request) {
        return verify(request);
    }

    // 세 인증 확인 경로는 같은 규칙(코드 일치 → 인증 완료 표시)을 쓴다.
    private ResponseEntity<Void> verify(VerifyCodeRequest request) {
        verifyCodeUseCase.execute(request.phoneNumber(), request.code());
        return ResponseEntity.ok().build();
    }
}
