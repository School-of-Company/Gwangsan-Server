package team.startup.gwangsan.application.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.member.data.UpdateMyPlaceCommand;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.application.member.port.out.ValidatePlacePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMyPlaceService 단위 테스트")
class UpdateMyPlaceServiceTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    @Mock private LoadMemberPort loadMemberPort;
    @Mock private SaveMemberPort saveMemberPort;
    @Mock private ValidatePlacePort validatePlacePort;

    private UpdateMyPlaceService service() {
        return new UpdateMyPlaceService(loadMemberPort, saveMemberPort, validatePlacePort);
    }

    @Nested
    @DisplayName("execute() 는")
    class Describe_execute {

        @Test
        @DisplayName("지점 존재를 확인한 뒤 회원 상세의 지점을 바꾸고 저장한다")
        void it_changes_place_after_validation() {
            MemberDetail detail = MemberDetail.applicant(3, 2, "설명");
            when(loadMemberPort.findDetail(1L)).thenReturn(Optional.of(detail));

            service().execute(new UpdateMyPlaceCommand(ACTOR, 4));

            verify(validatePlacePort).requireExists(4);
            assertThat(detail.getPlaceId()).isEqualTo(4);
            verify(saveMemberPort).saveDetail(detail);
        }

        @Test
        @DisplayName("존재하지 않는 지점이면 지점 저장 없이 PlaceNotFound 계열 예외를 던진다")
        void it_propagates_place_not_found() {
            doThrow(new team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException())
                    .when(validatePlacePort).requireExists(999);

            assertThatThrownBy(() -> service().execute(new UpdateMyPlaceCommand(ACTOR, 999)))
                    .isInstanceOf(team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException.class);
            verifyNoInteractions(loadMemberPort, saveMemberPort);
        }

        @Test
        @DisplayName("회원 상세가 없으면 NotFoundMemberDetailException을 던진다")
        void it_throws_not_found_member_detail_exception() {
            when(loadMemberPort.findDetail(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(new UpdateMyPlaceCommand(ACTOR, 4)))
                    .isInstanceOf(NotFoundMemberDetailException.class);
            verify(saveMemberPort, never()).saveDetail(any());
        }
    }
}
