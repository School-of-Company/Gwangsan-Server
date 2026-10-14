package team.startup.gwangsan.application.block.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.block.data.BlockedMember;
import team.startup.gwangsan.application.block.port.out.LoadMemberBlockPort;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockQueryService 단위 테스트")
class BlockQueryServiceTest {

    @Mock private LoadMemberBlockPort loadMemberBlockPort;
    @InjectMocks private BlockQueryService service;

    @Nested
    @DisplayName("query() 는")
    class Describe_query {

        @Test
        @DisplayName("it_차단한 회원 목록을 그대로 반환한다")
        void it_returns_blocked_members() {
            List<BlockedMember> blocked = List.of(new BlockedMember(10L, "차단유저A"), new BlockedMember(20L, "차단유저B"));
            when(loadMemberBlockPort.findBlockedMembers(1L)).thenReturn(blocked);

            assertThat(service.query(1L)).containsExactlyElementsOf(blocked);
        }
    }

    @Nested
    @DisplayName("validateNotBlocked() 는")
    class Describe_validateNotBlocked {

        @Test
        @DisplayName("it_어느 방향이든 차단 관계가 있으면 BlockedMemberException 을 던진다")
        void it_throws_blocked_member_exception() {
            when(loadMemberBlockPort.existsBetween(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> service.validateNotBlocked(1L, 2L)).isInstanceOf(BlockedMemberException.class);
        }

        @Test
        @DisplayName("it_차단 관계가 없으면 통과한다")
        void it_passes_without_block() {
            when(loadMemberBlockPort.existsBetween(1L, 2L)).thenReturn(false);

            assertThatCode(() -> service.validateNotBlocked(1L, 2L)).doesNotThrowAnyException();
        }
    }
}
