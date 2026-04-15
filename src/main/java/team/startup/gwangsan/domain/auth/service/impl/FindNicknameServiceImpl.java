package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotCompletedException;
import team.startup.gwangsan.domain.auth.exception.SmsAuthNotFoundException;
import team.startup.gwangsan.domain.auth.presentation.dto.request.FindNicknameRequest;
import team.startup.gwangsan.domain.auth.presentation.dto.response.FindNicknameResponse;
import team.startup.gwangsan.domain.auth.service.FindNicknameService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.redis.RedisUtil;

@Service
@RequiredArgsConstructor
public class FindNicknameServiceImpl implements FindNicknameService {

    private static final String VERIFIED_KEY_PREFIX = "sms:verified:";

    private final MemberRepository memberRepository;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(readOnly = true)
    public FindNicknameResponse execute(FindNicknameRequest request) {
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

        redisUtil.delete(verifiedKey);

        return new FindNicknameResponse(member.getNickname());
    }
}
