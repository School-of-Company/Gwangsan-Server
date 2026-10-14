package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.trade.data.ApproveTradeCancelCommand;
import team.startup.gwangsan.application.trade.port.in.ApproveTradeCancelUseCase;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.trade.exception.NotFoundTradeCancelException;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApproveTradeCancelServiceImpl 단위 테스트")
class ApproveTradeCancelServiceImplTest {

    @InjectMocks private ApproveTradeCancelServiceImpl service;

    @Mock private AdminAlertRepository adminAlertRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private MemberDetailJpaRepository memberDetailRepository;
    @Mock private ValidatePlaceUtil validatePlaceUtil;
    @Mock private ApproveTradeCancelUseCase approveTradeCancelUseCase;

    private MemberJpaEntity admin;
    private MemberDetailJpaEntity adminDetail;

    private void givenAlert() {
        admin = mock(MemberJpaEntity.class);
        when(admin.getId()).thenReturn(1L);
        when(memberUtil.getCurrentMember()).thenReturn(admin);
        AdminAlert alert = mock(AdminAlert.class);
        when(alert.getSourceId()).thenReturn(20L);
        when(adminAlertRepository.findByIdWithMember(5L)).thenReturn(Optional.of(alert));
        adminDetail = mock(MemberDetailJpaEntity.class);
        when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
    }

    /** trade use case 가 잠금 후 구매자/판매자로 인가 콜백을 호출하는 것을 흉내 낸다. */
    private ApproveTradeCancelCommand executeAndCapture() {
        service.execute(5L);
        ArgumentCaptor<ApproveTradeCancelCommand> command = ArgumentCaptor.forClass(ApproveTradeCancelCommand.class);
        verify(approveTradeCancelUseCase).execute(command.capture());
        return command.getValue();
    }

    @Nested
    @DisplayName("정상 승인 시")
    class Context_success {

        @Test
        @DisplayName("알림의 sourceId 로 trade 의 철회 승인 command 에 위임한다")
        void it_delegates_to_trade_use_case() {
            givenAlert();

            assertThat(executeAndCapture().tradeCancelId()).isEqualTo(20L);
        }

        @Test
        @DisplayName("인가 콜백은 구매자와 판매자 모두 관리자와 같은 지역인지 검증한다")
        void it_validates_both_participants_in_callback() {
            givenAlert();
            MemberDetailJpaEntity buyerDetail = mock(MemberDetailJpaEntity.class);
            MemberDetailJpaEntity sellerDetail = mock(MemberDetailJpaEntity.class);
            when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(buyerDetail));
            when(memberDetailRepository.findById(3L)).thenReturn(Optional.of(sellerDetail));

            executeAndCapture().authorizer().authorize(2L, 3L);

            verify(validatePlaceUtil).validateSamePlace(admin, adminDetail, buyerDetail);
            verify(validatePlaceUtil).validateSamePlace(admin, adminDetail, sellerDetail);
        }

        @Test
        @DisplayName("지역이 다르면 인가 콜백이 예외를 던져 trade 가 철회를 반영하지 않게 한다")
        void it_fails_callback_on_region_mismatch() {
            givenAlert();
            MemberDetailJpaEntity buyerDetail = mock(MemberDetailJpaEntity.class);
            when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(buyerDetail));
            when(memberDetailRepository.findById(3L)).thenReturn(Optional.of(mock(MemberDetailJpaEntity.class)));
            doThrow(new PlaceMismatchException()).when(validatePlaceUtil).validateSamePlace(admin, adminDetail, buyerDetail);

            ApproveTradeCancelCommand command = executeAndCapture();

            assertThatThrownBy(() -> command.authorizer().authorize(2L, 3L)).isInstanceOf(PlaceMismatchException.class);
        }
    }

    @Test
    @DisplayName("알림이 없으면 NotFoundAdminAlertException 을 던지고 trade 를 호출하지 않는다")
    void it_requires_alert() {
        when(memberUtil.getCurrentMember()).thenReturn(mock(MemberJpaEntity.class));
        when(adminAlertRepository.findByIdWithMember(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(5L)).isInstanceOf(NotFoundAdminAlertException.class);
        verifyNoInteractions(approveTradeCancelUseCase);
    }

    @Test
    @DisplayName("trade 의 오류(철회 요청 없음 등)를 그대로 전달한다")
    void it_propagates_trade_errors() {
        givenAlert();
        doThrow(new NotFoundTradeCancelException()).when(approveTradeCancelUseCase).execute(any());

        assertThatThrownBy(() -> service.execute(5L)).isInstanceOf(NotFoundTradeCancelException.class);
    }
}
