package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.exception.NotFoundAdminAlertException;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAdminAlertServiceImpl 단위 테스트")
class DeleteAdminAlertServiceImplTest {

    @InjectMocks
    private DeleteAdminAlertServiceImpl service;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    @Mock
    private MemberDetailRepository memberDetailRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("AdminAlert를 삭제한다")
            void it_deletes_admin_alert() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                Member requester = mock(Member.class);
                when(requester.getId()).thenReturn(2L);
                AdminAlert alert = mock(AdminAlert.class);
                when(alert.getRequester()).thenReturn(requester);
                MemberDetail alertMemberDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findById(10L)).thenReturn(Optional.of(alert));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(alertMemberDetail));

                service.execute(10L);

                verify(adminAlertRepository).delete(alert);
                verify(validatePlaceUtil).validateSamePlace(admin, adminDetail, alertMemberDetail);
            }
        }

        @Nested
        @DisplayName("AdminAlert가 존재하지 않을 때")
        class Context_with_alert_not_found {

            @Test
            @DisplayName("NotFoundAdminAlertException을 던진다")
            void it_throws_not_found_admin_alert_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetail adminDetail = mock(MemberDetail.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(adminAlertRepository.findById(10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundAdminAlertException.class);
            }
        }

        @Nested
        @DisplayName("관리자의 MemberDetail이 없을 때")
        class Context_with_admin_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberDetailException을 던진다")
            void it_throws_not_found_member_detail_exception() {
                Member admin = mock(Member.class);
                when(admin.getId()).thenReturn(1L);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(10L))
                        .isInstanceOf(NotFoundMemberDetailException.class);
            }
        }
    }
}
