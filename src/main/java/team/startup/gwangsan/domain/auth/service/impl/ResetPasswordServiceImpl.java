package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.ResetPasswordRequest;
import team.startup.gwangsan.domain.auth.service.ResetPasswordService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.redis.RedisUtil;

@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void execute(ResetPasswordRequest request) {
        String phoneNumber = request.phoneNumber();
        String verifiedKey = VERIFIED_KEY_PREFIX + phoneNumber;

        Boolean verified = redisUtil.get(verifiedKey, Boolean.class);
        if (verified == null) {
            throw new SmsAuthNotFoundException();
        }

        if (!Boolean.TRUE.equals(verified)) {
            throw new SmsAuthNotCompletedException();
        }

        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(NotFoundMemberException::new);

        member.changePassword(passwordEncoder.encode(request.newPassword()));

        redisUtil.delete(verifiedKey);
    }
}
