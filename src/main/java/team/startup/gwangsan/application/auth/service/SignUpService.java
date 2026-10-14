package team.startup.gwangsan.application.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.auth.data.NewAccount;
import team.startup.gwangsan.application.auth.data.SignUpCommand;
import team.startup.gwangsan.application.auth.port.in.SignUpUseCase;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.application.auth.port.out.SignUpAlertPort;
import team.startup.gwangsan.application.auth.port.out.SignUpLocationPort;
import team.startup.gwangsan.domain.auth.exception.BannedPhoneNumberException;
import team.startup.gwangsan.domain.auth.exception.DongNotFoundException;
import team.startup.gwangsan.domain.auth.exception.DuplicateNicknameException;
import team.startup.gwangsan.domain.auth.exception.DuplicatePhoneNumberException;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.domain.auth.exception.RecommenderNotFoundException;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.PhoneVerification;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private final AccountPort accountPort;
    private final PhoneVerificationPort phoneVerificationPort;
    private final SignUpLocationPort signUpLocationPort;
    private final PasswordHashPort passwordHashPort;
    private final SignUpAlertPort signUpAlertPort;

    /** 검증 순서가 오류 응답을 결정한다: 데모 → 강제탈퇴 → 번호 중복 → 별칭 중복 → SMS → 동 → 지점 → 추천인. */
    @Override
    @Transactional
    public void execute(SignUpCommand command) {
        // ponytail: 데모 번호는 App Store 심사용. 데모 회원이 이미 존재하고 phone_number가 unique라
        // 실제 저장이 불가능하므로 검증/저장 없이 성공 처리한다. 리뷰어는 Review Notes의 데모 계정으로 로그인한다.
        if (phoneVerificationPort.isDemoNumber(command.phoneNumber())) {
            return;
        }

        validateBannedPhoneNumber(command.phoneNumber());
        if (accountPort.existsByPhoneNumber(command.phoneNumber())) {
            throw new DuplicatePhoneNumberException();
        }
        if (accountPort.existsByNickname(command.nickname())) {
            throw new DuplicateNicknameException();
        }
        PhoneVerification.assertCompleted(phoneVerificationPort.findVerified(command.phoneNumber()));

        Integer dongId = signUpLocationPort.findDongIdByName(command.dongName())
                .orElseThrow(DongNotFoundException::new);
        if (!signUpLocationPort.existsPlace(command.placeId())) {
            throw new PlaceNotFoundException();
        }
        Account recommender = accountPort.findByNickname(command.recommender())
                .orElseThrow(RecommenderNotFoundException::new);

        Long memberId = accountPort.register(new NewAccount(command.name(), command.nickname(),
                passwordHashPort.encode(command.password()), command.phoneNumber(), recommender.id(),
                dongId, command.placeId(), command.description(), command.specialties()));

        signUpAlertPort.notifySignUp(memberId, recommender.id());
        phoneVerificationPort.consume(command.phoneNumber());
    }

    private void validateBannedPhoneNumber(String phoneNumber) {
        if (accountPort.isBannedPhoneNumber(phoneNumber)) {
            // 이 검증은 recommender(추천인) 필드와 무관하게 가입 신청자 본인의 phoneNumber만 본다.
            // 응답 메시지만 보면 추천인 조회 실패로 오인하기 쉬워 운영 확인용으로 남긴다.
            log.warn("회원가입 차단: 강제 탈퇴 이력이 있는 휴대폰 번호로 가입 시도 - phoneNumber={}",
                    phoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
            throw new BannedPhoneNumberException();
        }
    }
}
