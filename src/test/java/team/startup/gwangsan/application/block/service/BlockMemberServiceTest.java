package team.startup.gwangsan.application.block.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.block.data.MemberBlockCommand;
import team.startup.gwangsan.application.block.port.out.LoadBlockTargetPort;
import team.startup.gwangsan.application.block.port.out.LoadMemberBlockPort;
import team.startup.gwangsan.application.block.port.out.SaveMemberBlockPort;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;
import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;
import team.startup.gwangsan.domain.block.model.MemberBlock;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("차단/해제 유스케이스 단위 테스트")
class BlockMemberServiceTest {

    @Mock private LoadMemberBlockPort loadMemberBlockPort;
    @Mock private SaveMemberBlockPort saveMemberBlockPort;
    @Mock private LoadBlockTargetPort loadBlockTargetPort;

    @InjectMocks private BlockMemberService blockMemberService;
    @InjectMocks private UnblockMemberService unblockMemberService;

    @Nested
    @DisplayName("BlockMemberService.execute() 는")
    class Describe_block {

        @Test
        @DisplayName("it_정상 차단이면 대상 회원 확인 후 저장한다")
        void it_saves_block() {
            blockMemberService.execute(new MemberBlockCommand(1L, 2L));

            verify(loadBlockTargetPort).checkMemberExists(2L);
            verify(saveMemberBlockPort).save(new MemberBlock(1L, 2L));
        }

        @Test
        @DisplayName("it_본인을 차단하면 SelfBlockNotAllowedException 을 던지고 저장소를 조회하지 않는다")
        void it_throws_self_block_not_allowed_exception() {
            assertThatThrownBy(() -> blockMemberService.execute(new MemberBlockCommand(1L, 1L)))
                    .isInstanceOf(SelfBlockNotAllowedException.class);

            verifyNoInteractions(loadMemberBlockPort, saveMemberBlockPort, loadBlockTargetPort);
        }

        @Test
        @DisplayName("it_이미 차단했으면 대상 회원 확인 전에 AlreadyBlockedException 을 던진다")
        void it_throws_already_blocked_exception() {
            when(loadMemberBlockPort.exists(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> blockMemberService.execute(new MemberBlockCommand(1L, 2L)))
                    .isInstanceOf(AlreadyBlockedException.class);

            verifyNoInteractions(loadBlockTargetPort, saveMemberBlockPort);
        }

        @Test
        @DisplayName("it_대상 회원이 없으면 NotFoundMemberException 을 던지고 저장하지 않는다")
        void it_throws_not_found_member_exception() {
            doThrow(new NotFoundMemberException()).when(loadBlockTargetPort).checkMemberExists(2L);

            assertThatThrownBy(() -> blockMemberService.execute(new MemberBlockCommand(1L, 2L)))
                    .isInstanceOf(NotFoundMemberException.class);

            verify(saveMemberBlockPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("UnblockMemberService.execute() 는")
    class Describe_unblock {

        @Test
        @DisplayName("it_차단 내역이 있으면 삭제한다")
        void it_deletes_block() {
            when(saveMemberBlockPort.delete(1L, 2L)).thenReturn(true);

            unblockMemberService.execute(new MemberBlockCommand(1L, 2L));

            verify(saveMemberBlockPort).delete(1L, 2L);
        }

        @Test
        @DisplayName("it_차단 내역이 없으면 NotFoundBlockException 을 던진다")
        void it_throws_not_found_block_exception() {
            when(saveMemberBlockPort.delete(1L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> unblockMemberService.execute(new MemberBlockCommand(1L, 2L)))
                    .isInstanceOf(NotFoundBlockException.class);
        }
    }
}
