package team.startup.gwangsan.domain.sms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;
import team.startup.gwangsan.domain.sms.exception.NotMatchRandomCodeException;
import team.startup.gwangsan.domain.sms.presentation.dto.VerifyCodeRequest;
import team.startup.gwangsan.domain.sms.repository.SmsAuthRepository;
import team.startup.gwangsan.domain.sms.service.VerifyResetPasswordCodeService;

@Service
@RequiredArgsConstructor
public class VerifyResetPasswordCodeServiceImpl implements VerifyResetPasswordCodeService {

    private final SmsAuthRepository smsAuthRepository;

    @Override
    @Transactional
    public void execute(VerifyCodeRequest request) {
        SmsAuthEntity smsAuthEntity = smsAuthRepository.findById(request.phoneNumber())
                .orElseThrow(SmsAuthNotFoundException::new);

        if (!smsAuthEntity.getRandomValue().equals(request.code())) {
            throw new NotMatchRandomCodeException();
        }

        smsAuthEntity.changeAuthentication();
        smsAuthRepository.save(smsAuthEntity);
    }
}
