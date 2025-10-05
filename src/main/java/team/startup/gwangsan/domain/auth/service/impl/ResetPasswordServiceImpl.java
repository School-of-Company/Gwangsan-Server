package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.service.ResetPasswordService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.sms.entity.SmsAuthEntity;
import team.startup.gwangsan.domain.sms.repository.SmsAuthRepository;

@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final MemberRepository memberRepository;
    private final SmsAuthRepository smsAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(team.startup.gwangsan.domain.auth.presentation.dto.request.ResetPasswordRequest request) {
        SmsAuthEntity smsAuthEntity = smsAuthRepository.findById(request.phoneNumber())
                .orElseThrow(SmsAuthNotFoundException::new);

        if (!smsAuthEntity.getAuthentication()) {
            throw new SmsAuthNotCompletedException();
        }

        Member member = memberRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(NotFoundMemberException::new);

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }
}

