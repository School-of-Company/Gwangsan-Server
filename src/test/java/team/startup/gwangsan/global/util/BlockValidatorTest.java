package team.startup.gwangsan.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.block.exception.BlockedMemberException;
import team.startup.gwangsan.domain.block.repository.MemberBlockRepository;
import team.startup.gwangsan.domain.member.entity.Member;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockValidator 단위 테스트")
class BlockValidatorTest {

    @InjectMocks private BlockValidator blockValidator;

    @Mock private MemberBlockRepository memberBlockRepository;

    @Nested
    @DisplayName("validate(Long, Long) 메서드는")
    class Describe_validate_by_id {

        @Nested
        @DisplayName("차단 관계가 있을 때")
        class Context_with_block_exists {

            @Test
            @DisplayName("BlockedMemberException을 던진다")
            void it_throws_blocked_member_exception() {
                when(memberBlockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);

                assertThatThrownBy(() -> blockValidator.validate(1L, 2L))
                        .isInstanceOf(BlockedMemberException.class);
            }
        }

        @Nested
        @DisplayName("차단 관계가 없을 때")
        class Context_with_no_block {

            @Test
            @DisplayName("예외 없이 통과한다")
            void it_passes_without_exception() {
                when(memberBlockRepository.existsBlockBetween(1L, 2L)).thenReturn(false);

                assertThatCode(() -> blockValidator.validate(1L, 2L))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("validate(Member, Member) 메서드는")
    class Describe_validate_by_member {

        @Nested
        @DisplayName("차단 관계가 있을 때")
        class Context_with_block_exists {

            @Test
            @DisplayName("BlockedMemberException을 던진다")
            void it_throws_blocked_member_exception() {
                Member current = mock(Member.class);
                Member target = mock(Member.class);
                when(current.getId()).thenReturn(1L);
                when(target.getId()).thenReturn(2L);
                when(memberBlockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);

                assertThatThrownBy(() -> blockValidator.validate(current, target))
                        .isInstanceOf(BlockedMemberException.class);
            }
        }

        @Nested
        @DisplayName("차단 관계가 없을 때")
        class Context_with_no_block {

            @Test
            @DisplayName("예외 없이 통과한다")
            void it_passes_without_exception() {
                Member current = mock(Member.class);
                Member target = mock(Member.class);
                when(current.getId()).thenReturn(1L);
                when(target.getId()).thenReturn(2L);
                when(memberBlockRepository.existsBlockBetween(1L, 2L)).thenReturn(false);

                assertThatCode(() -> blockValidator.validate(current, target))
                        .doesNotThrowAnyException();
            }
        }
    }
}