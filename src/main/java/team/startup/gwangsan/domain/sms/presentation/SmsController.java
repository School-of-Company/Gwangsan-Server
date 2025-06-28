package team.startup.gwangsan.domain.sms.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.sms.presentation.dto.SendSmsRequest;
import team.startup.gwangsan.domain.sms.service.SendSmsService;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SendSmsService sendSmsService;

    @PostMapping
    public ResponseEntity<SingleMessageSentResponse> sendSms(@RequestBody @Valid SendSmsRequest request) {
        SingleMessageSentResponse response = sendSmsService.execute(request);
        return ResponseEntity.ok(response);
    }

}
