package team.startup.gwangsan.domain.block.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.exception.AlreadyBlockedException;
import team.startup.gwangsan.domain.block.exception.SelfBlockNotAllowedException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockMemberServiceImpl 단위 테스트")
class BlockMemberServiceImplTest {

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @InjectMocks
    private BlockMemberServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("정상적으로 차단하면 MemberBlock 을 저장한다")
        void it_saves_block() {
            // given
            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Member targetMember = mock(Member.class);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(targetMember));
            when(memberBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);

            ArgumentCaptor<MemberBlock> captor = ArgumentCaptor.forClass(MemberBlock.class);

            // when
            service.execute(2L);

            // then
            verify(memberBlockRepository).save(captor.capture());
            MemberBlock saved = captor.getValue();
            assertThat(saved.getBlocker()).isEqualTo(currentMember);
            assertThat(saved.getBlocked()).isEqualTo(targetMember);
        }

        @Test
        @DisplayName("본인을 차단하면 SelfBlockNotAllowedException 을 던진다")
        void it_throws_SelfBlockNotAllowedException_when_self_block() {
            // given
            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            // when & then
            assertThrows(SelfBlockNotAllowedException.class,
                    () -> service.execute(1L));

            verifyNoInteractions(memberBlockRepository);
        }

        @Test
        @DisplayName("이미 차단한 유저면 AlreadyBlockedException 을 던진다")
        void it_throws_AlreadyBlockedException_when_already_blocked() {
            // given
            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            when(memberBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            // when & then
            assertThrows(AlreadyBlockedException.class,
                    () -> service.execute(2L));

            verify(memberBlockRepository, never()).save(any());
        }
    }
}
