package team.startup.gwangsan.domain.block.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.entity.MemberBlock;
import team.startup.gwangsan.domain.block.presentation.dto.response.GetBlockedMemberResponse;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetBlockListServiceImpl 단위 테스트")
class GetBlockListServiceImplTest {

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @InjectMocks
    private GetBlockListServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("차단 목록을 반환한다")
        void it_returns_block_list() {
            // given
            Member currentMember = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            Member blocked1 = mock(Member.class);
            when(blocked1.getId()).thenReturn(10L);
            when(blocked1.getNickname()).thenReturn("차단유저A");

            Member blocked2 = mock(Member.class);
            when(blocked2.getId()).thenReturn(20L);
            when(blocked2.getNickname()).thenReturn("차단유저B");

            MemberBlock block1 = mock(MemberBlock.class);
            when(block1.getBlocked()).thenReturn(blocked1);

            MemberBlock block2 = mock(MemberBlock.class);
            when(block2.getBlocked()).thenReturn(blocked2);

            when(memberBlockRepository.findAllByBlocker(currentMember))
                    .thenReturn(List.of(block1, block2));

            // when
            List<GetBlockedMemberResponse> result = service.execute();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).memberId()).isEqualTo(10L);
            assertThat(result.get(0).nickname()).isEqualTo("차단유저A");
            assertThat(result.get(1).memberId()).isEqualTo(20L);
            assertThat(result.get(1).nickname()).isEqualTo("차단유저B");
        }

        @Test
        @DisplayName("차단 목록이 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_blocks() {
            // given
            Member currentMember = mock(Member.class);
            when(memberUtil.getCurrentMember()).thenReturn(currentMember);

            when(memberBlockRepository.findAllByBlocker(currentMember)).thenReturn(List.of());

            // when
            List<GetBlockedMemberResponse> result = service.execute();

            // then
            assertThat(result).isEmpty();
        }
    }
}
