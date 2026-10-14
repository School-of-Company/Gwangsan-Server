package team.startup.gwangsan.application.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.auth.data.NewAccount;
import team.startup.gwangsan.application.auth.data.SignUpCommand;
import team.startup.gwangsan.application.auth.port.out.AccountPort;
import team.startup.gwangsan.application.auth.port.out.PasswordHashPort;
import team.startup.gwangsan.application.auth.port.out.PhoneVerificationPort;
import team.startup.gwangsan.application.auth.port.out.SignUpAlertPort;
import team.startup.gwangsan.application.auth.port.out.SignUpLocationPort;
import team.startup.gwangsan.domain.auth.exception.*;
import team.startup.gwangsan.domain.auth.model.Account;
import team.startup.gwangsan.domain.auth.model.AccountStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignUpService 단위 테스트")
class SignUpServiceTest {

    @Mock private AccountPort accountPort;
    @Mock private PhoneVerificationPort phoneVerificationPort;
    @Mock private SignUpLocationPort signUpLocationPort;
    @Mock private PasswordHashPort passwordHashPort;
    @Mock private SignUpAlertPort signUpAlertPort;

    private SignUpService service;

    @BeforeEach
    void setUp() {
        service = new SignUpService(accountPort, phoneVerificationPort, signUpLocationPort, passwordHashPort, signUpAlertPort);
    }

    private SignUpCommand validCommand() {
        return new SignUpCommand("홍길동", "테스터일", "password", "01012345678",
                "광산동", 1, List.of("Java", "Spring"), "추천인닉", "자기소개");
    }

    private void passUntilVerification(SignUpCommand command, Boolean verified) {
        when(accountPort.existsByPhoneNumber(command.phoneNumber())).thenReturn(false);
        when(accountPort.existsByNickname(command.nickname())).thenReturn(false);
        when(phoneVerificationPort.findVerified(command.phoneNumber())).thenReturn(verified);
    }

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("데모 번호일 때")
        class Context_with_demo_phone_number {

            @Test
            @DisplayName("검증·저장 없이 즉시 반환한다")
            void it_returns_immediately_without_saving_member() {
                SignUpCommand command = new SignUpCommand("심사용", "심사용닉", "password", "01011111111",
                        "광산동", 1, List.of("Java"), "추천인닉", "자기소개");
                when(phoneVerificationPort.isDemoNumber("01011111111")).thenReturn(true);

                service.execute(command);

                verifyNoInteractions(accountPort, signUpLocationPort, passwordHashPort, signUpAlertPort);
                verify(phoneVerificationPort, never()).consume(any());
            }
        }

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("회원을 등록하고 알림을 보낸 뒤 인증 표시를 소비한다")
            void it_registers_member_and_notifies() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, true);
                when(signUpLocationPort.findDongIdByName("광산동")).thenReturn(Optional.of(3));
                when(signUpLocationPort.existsPlace(1)).thenReturn(true);
                when(accountPort.findByNickname("추천인닉")).thenReturn(Optional.of(
                        new Account(9L, "추천인닉", "01000000000", "pw", "ROLE_USER", AccountStatus.ACTIVE)));
                when(passwordHashPort.encode("password")).thenReturn("encoded");
                when(accountPort.register(any())).thenReturn(10L);

                service.execute(command);

                ArgumentCaptor<NewAccount> account = ArgumentCaptor.forClass(NewAccount.class);
                verify(accountPort).register(account.capture());
                assertThat(account.getValue()).isEqualTo(new NewAccount("홍길동", "테스터일", "encoded",
                        "01012345678", 9L, 3, 1, "자기소개", List.of("Java", "Spring")));
                InOrder order = inOrder(signUpAlertPort, phoneVerificationPort);
                order.verify(signUpAlertPort).notifySignUp(10L, 9L);
                order.verify(phoneVerificationPort).consume("01012345678");
            }
        }

        @Nested
        @DisplayName("차단된 탈퇴 번호일 때")
        class Context_with_banned_phone_number {

            @Test
            @DisplayName("BannedPhoneNumberException을 던지고 이후 가입 처리를 시작하지 않는다")
            void it_throws_banned_phone_number_exception_before_downstream_actions() {
                SignUpCommand command = validCommand();
                when(accountPort.isBannedPhoneNumber(command.phoneNumber())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(command))
                        .isInstanceOf(BannedPhoneNumberException.class);

                verify(accountPort).isBannedPhoneNumber(command.phoneNumber());
                verifyNoMoreInteractions(accountPort);
                verifyNoInteractions(signUpLocationPort, passwordHashPort, signUpAlertPort);
                verify(phoneVerificationPort, never()).findVerified(any());
            }

            @Test
            @DisplayName("추천인 닉네임이 실제 존재해도, 가입 신청자 본인 번호가 차단되어 있으면 추천인 조회 없이 실패한다")
            void it_fails_regardless_of_recommender_validity_when_own_phone_is_banned() {
                SignUpCommand command = validCommand();
                when(accountPort.isBannedPhoneNumber(command.phoneNumber())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(command))
                        .isInstanceOf(BannedPhoneNumberException.class)
                        .extracting(ex -> ((BannedPhoneNumberException) ex).getErrorCode().getMessage())
                        .isEqualTo("강제 탈퇴 처리된 휴대폰 번호로는 재가입할 수 없습니다.");

                verify(accountPort, never()).findByNickname(command.recommender());
            }
        }

        @Nested
        @DisplayName("전화번호가 중복일 때")
        class Context_with_duplicate_phone {

            @Test
            @DisplayName("DuplicatePhoneNumberException을 던진다")
            void it_throws_duplicate_phone_exception() {
                SignUpCommand command = validCommand();
                when(accountPort.existsByPhoneNumber(command.phoneNumber())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DuplicatePhoneNumberException.class);
            }
        }

        @Nested
        @DisplayName("닉네임이 중복일 때")
        class Context_with_duplicate_nickname {

            @Test
            @DisplayName("DuplicateNicknameException을 던진다")
            void it_throws_duplicate_nickname_exception() {
                SignUpCommand command = validCommand();
                when(accountPort.existsByPhoneNumber(command.phoneNumber())).thenReturn(false);
                when(accountPort.existsByNickname(command.nickname())).thenReturn(true);

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DuplicateNicknameException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증 정보가 없을 때")
        class Context_with_sms_auth_not_found {

            @Test
            @DisplayName("SmsAuthNotFoundException을 던진다")
            void it_throws_sms_auth_not_found_exception() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, null);

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(SmsAuthNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("SMS 인증이 완료되지 않았을 때")
        class Context_with_sms_not_completed {

            @Test
            @DisplayName("SmsAuthNotCompletedException을 던진다")
            void it_throws_sms_not_completed_exception() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, false);

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(SmsAuthNotCompletedException.class);
            }
        }

        @Nested
        @DisplayName("동 정보가 없을 때")
        class Context_with_dong_not_found {

            @Test
            @DisplayName("DongNotFoundException을 던진다")
            void it_throws_dong_not_found_exception() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, true);
                when(signUpLocationPort.findDongIdByName(command.dongName())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(DongNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("지점이 없을 때")
        class Context_with_place_not_found {

            @Test
            @DisplayName("PlaceNotFoundException을 던진다")
            void it_throws_place_not_found_exception() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, true);
                when(signUpLocationPort.findDongIdByName(command.dongName())).thenReturn(Optional.of(3));
                when(signUpLocationPort.existsPlace(command.placeId())).thenReturn(false);

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(PlaceNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("추천인이 없을 때")
        class Context_with_recommender_not_found {

            @Test
            @DisplayName("RecommenderNotFoundException을 던지고 회원을 만들지 않는다")
            void it_throws_recommender_not_found_exception() {
                SignUpCommand command = validCommand();
                passUntilVerification(command, true);
                when(signUpLocationPort.findDongIdByName(command.dongName())).thenReturn(Optional.of(3));
                when(signUpLocationPort.existsPlace(command.placeId())).thenReturn(true);
                when(accountPort.findByNickname(command.recommender())).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(command)).isInstanceOf(RecommenderNotFoundException.class);
                verify(accountPort, never()).register(any());
                verify(phoneVerificationPort, never()).consume(any());
            }
        }
    }
}
