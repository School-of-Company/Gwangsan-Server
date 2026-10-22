package team.startup.gwangsan.application.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.member.data.UpdateMyPlaceCommand;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.application.member.port.out.MemberPlacePort;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.model.MemberDetail;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("본인 지점 변경 서비스")
class UpdateMyPlaceServiceTest {
    private final LoadMemberPort load = mock(LoadMemberPort.class);
    private final SaveMemberPort save = mock(SaveMemberPort.class);
    private final MemberPlacePort places = mock(MemberPlacePort.class);
    private final UpdateMyPlaceService service = new UpdateMyPlaceService(load, save, places);
    private final ActorContext actor = new ActorContext(12L, ActorContext.Role.ROLE_USER);

    @Nested
    @DisplayName("지점 변경은")
    class Describe_update {
        @Test
        @DisplayName("it_지점만 변경하며 같은 지점 재선택을 허용한다")
        void it_changes_only_place_and_allows_same_place() {
            MemberDetail detail = new MemberDetail(12L, 1, 2, 300, 5, "소개");
            when(load.findDetail(12L)).thenReturn(Optional.of(detail));
            service.execute(new UpdateMyPlaceCommand(actor, 7));
            assertThat(detail.getPlaceId()).isEqualTo(7);
            assertThat(detail.getDongId()).isEqualTo(1);
            assertThat(detail.getGwangsan()).isEqualTo(300);
            assertThat(detail.getLight()).isEqualTo(5);
            assertThat(detail.getDescription()).isEqualTo("소개");
            service.execute(new UpdateMyPlaceCommand(actor, 7));
            verify(places, times(2)).validate(7);
            verify(save, times(2)).saveDetail(detail);
            verifyNoMoreInteractions(save);
        }

        @Test
        @DisplayName("it_지점 검증 실패 시 기존 지점을 보존하고 저장하지 않는다")
        void it_does_not_save_when_place_validation_fails() {
            MemberDetail detail = new MemberDetail(12L, 1, 2, 300, 5, "소개");
            when(load.findDetail(12L)).thenReturn(Optional.of(detail));
            IllegalArgumentException failure = new IllegalArgumentException("invalid place");
            doThrow(failure).when(places).validate(999);
            assertThatThrownBy(() -> service.execute(new UpdateMyPlaceCommand(actor, 999))).isSameAs(failure);
            assertThat(detail.getPlaceId()).isEqualTo(2);
            verifyNoInteractions(save);
        }

        @Test
        @DisplayName("it_회원 상세가 없으면 변경을 거절한다")
        void it_rejects_missing_member_detail() {
            when(load.findDetail(12L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.execute(new UpdateMyPlaceCommand(actor, 7)))
                    .isInstanceOf(NotFoundMemberDetailException.class);
            verifyNoInteractions(save, places);
        }
    }
}
