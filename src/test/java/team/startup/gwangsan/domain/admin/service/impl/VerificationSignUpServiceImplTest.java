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
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.WithdrawalRecordJpaRepository;
import team.startup.gwangsan.adapter.event.alert.CreateAlertEvent;
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
    private MemberDetailJpaRepository memberDetailRepository;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private WithdrawalRecordJpaRepository withdrawalRecordRepository;

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
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(2L);

                MemberJpaEntity member = mock(MemberJpaEntity.class);
                when(member.getPhoneNumber()).thenReturn("010-0000-0000");
                MemberJpaEntity recommender = mock(MemberJpaEntity.class);
                when(recommender.getId()).thenReturn(3L);
                when(member.getRecommender()).thenReturn(recommender);

                MemberDetailJpaEntity memberDetail = mock(MemberDetailJpaEntity.class);
                when(memberDetail.getMember()).thenReturn(member);

                MemberDetailJpaEntity recommenderDetail = mock(MemberDetailJpaEntity.class);

                WithdrawalRecordJpaEntity record = mock(WithdrawalRecordJpaEntity.class);
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
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getSourceId()).thenReturn(2L);

                MemberJpaEntity member = mock(MemberJpaEntity.class);
                when(member.getPhoneNumber()).thenReturn("010-1111-1111");
                MemberJpaEntity recommender = mock(MemberJpaEntity.class);
                when(recommender.getId()).thenReturn(3L);
                when(member.getRecommender()).thenReturn(recommender);

                MemberDetailJpaEntity memberDetail = mock(MemberDetailJpaEntity.class);
                when(memberDetail.getMember()).thenReturn(member);

                MemberDetailJpaEntity recommenderDetail = mock(MemberDetailJpaEntity.class);

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
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findByIdAndType(10L, AlertType.SIGN_UP)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }
    }
}
