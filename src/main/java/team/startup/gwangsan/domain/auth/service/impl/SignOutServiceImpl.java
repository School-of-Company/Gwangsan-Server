package team.startup.gwangsan.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.auth.repository.RefreshTokenRepository;
import team.startup.gwangsan.domain.auth.service.SignOutService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class SignOutServiceImpl implements SignOutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberUtil memberUtil;

    @Override
    public void execute() {
        String phoneNumber = memberUtil.getCurrentMember().getPhoneNumber();
        refreshTokenRepository.deleteById(phoneNumber);
    }
}

