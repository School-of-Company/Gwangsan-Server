package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.WithdrawalRecord;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.WithdrawalRecordRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationSignUpServiceImpl 단위 테스트")
class VerificationSignUpServiceImplTest {

    @InjectMocks
    private VerificationSignUpServiceImpl service;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private WithdrawalRecordRepository withdrawalRecordRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("탈퇴 이력이 있는 회원의 가입 승인 시")
        class Context_with_withdrawal_record {

            @Test
            @DisplayName("탈퇴 이력의 광산을 복원하고 기록을 삭제한다")
            void it_restores_gwangsan_from_withdrawal_record() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(2L);

                Member member = mock(Member.class);
                when(member.getPhoneNumber()).thenReturn("010-0000-0000");
                Member recommender = mock(Member.class);
                when(recommender.getId()).thenReturn(3L);
                when(member.getRecommender()).thenReturn(recommender);

                MemberDetail memberDetail = mock(MemberDetail.class);
                when(memberDetail.getMember()).thenReturn(member);

                MemberDetail recommenderDetail = mock(MemberDetail.class);

                WithdrawalRecord record = mock(WithdrawalRecord.class);
                when(record.getGwangsan()).thenReturn(5000);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findByIdAndType(10L, AlertType.SIGN_UP)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(memberDetail));
                when(memberDetailRepository.findById(3L)).thenReturn(Optional.of(recommenderDetail));
                when(withdrawalRecordRepository.findByPhoneNumber("010-0000-0000")).thenReturn(Optional.of(record));

                service.execute(10L);

                verify(memberDetail).plusGwangsan(5000);
                verify(withdrawalRecordRepository).delete(record);
                verify(member).updateMemberStatus(MemberStatus.ACTIVE);
                verify(adminAlertRepository).delete(alert);
                verify(applicationEventPublisher).publishEvent(any(CreateAlertEvent.class));
            }
        }

        @Nested
        @DisplayName("탈퇴 이력이 없는 신규 회원의 가입 승인 시")
        class Context_without_withdrawal_record {

            @Test
            @DisplayName("기본 가입 광산 10000을 지급한다")
            void it_gives_default_signup_gwangsan() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(2L);

                Member member = mock(Member.class);
                when(member.getPhoneNumber()).thenReturn("010-1111-1111");
                Member recommender = mock(Member.class);
                when(recommender.getId()).thenReturn(3L);
                when(member.getRecommender()).thenReturn(recommender);

                MemberDetail memberDetail = mock(MemberDetail.class);
                when(memberDetail.getMember()).thenReturn(member);

                MemberDetail recommenderDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findByIdAndType(10L, AlertType.SIGN_UP)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(memberDetail));
                when(memberDetailRepository.findById(3L)).thenReturn(Optional.of(recommenderDetail));
                when(withdrawalRecordRepository.findByPhoneNumber("010-1111-1111")).thenReturn(Optional.empty());

                service.execute(10L);

                verify(memberDetail).plusGwangsan(10000);
                verify(recommenderDetail).plusGwangsan(5000);
                verify(withdrawalRecordRepository, never()).delete(any());
            }
        }

        @Nested
        @DisplayName("SIGN_UP 타입 AlertId에 해당하는 알림이 없을 때")
        class Context_with_alert_not_found {

            @Test
            @DisplayName("NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_admin_alert_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findByIdAndType(10L, AlertType.SIGN_UP)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }
    }
}
