package team.startup.gwangsan.domain.block.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.exception.NotFoundBlockException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnblockMemberServiceImpl 단위 테스트")
class UnblockMemberServiceImplTest {

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @InjectMocks
    private UnblockMemberServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("차단 내역이 있으면 삭제한다")
        void it_deletes_block() {
            // given
            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            MemberBlock block = mock(MemberBlock.class);
            when(memberBlockRepository.findByBlockerIdAndBlockedId(1L, 2L))
                    .thenReturn(Optional.of(block));

            // when
            service.execute(2L);

            // then
            verify(memberBlockRepository).delete(block);
        }

        @Test
        @DisplayName("차단 내역이 없으면 NotFoundBlockException 을 던진다")
        void it_throws_NotFoundBlockException_when_block_not_found() {
            // given
            Member currentMember = mock(Member.class);
            when(currentMember.getId()).thenReturn(1L);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            when(memberBlockRepository.findByBlockerIdAndBlockedId(1L, 2L))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(NotFoundBlockException.class,
                    () -> service.execute(2L));

            verify(memberBlockRepository, never()).delete(any());
        }
    }
}
