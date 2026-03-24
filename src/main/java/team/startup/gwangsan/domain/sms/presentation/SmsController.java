package team.startup.gwangsan.domain.sms.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;
import team.startup.gwangsan.domain.sms.service.SendResetPasswordSmsService;
import team.startup.gwangsan.domain.sms.service.SendSmsService;
import team.startup.gwangsan.domain.sms.service.VerifyCodeService;
import team.startup.gwangsan.domain.sms.service.VerifyResetPasswordCodeService;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SendSmsService sendSmsService;
    private final VerifyCodeService verifyCodeService;
    private final SendResetPasswordSmsService sendResetPasswordSmsService;
    private final VerifyResetPasswordCodeService verifyResetPasswordCodeService;

    @PostMapping
    public ResponseEntity<SingleMessageSentResponse> sendSms(@RequestBody @Valid SendSmsRequest request) {
        SingleMessageSentResponse response = sendSmsService.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifySms(@RequestBody @Valid VerifyCodeRequest request) {
        verifyCodeService.execute(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password")
    public ResponseEntity<SingleMessageSentResponse> sendResetPasswordSms(@RequestBody @Valid SendSmsRequest request) {
        SingleMessageSentResponse response = sendResetPasswordSmsService.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/verify")
    public ResponseEntity<Void> verifyResetPasswordSms(@RequestBody @Valid VerifyCodeRequest request) {
        verifyResetPasswordCodeService.execute(request);
        return ResponseEntity.ok().build();
    }

}
