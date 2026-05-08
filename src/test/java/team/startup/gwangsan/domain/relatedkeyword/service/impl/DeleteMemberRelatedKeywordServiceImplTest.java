package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordForbiddenException;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordNotFoundException;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteMemberRelatedKeywordServiceImpl 단위 테스트")
class DeleteMemberRelatedKeywordServiceImplTest {

    @InjectMocks
    private DeleteMemberRelatedKeywordServiceImpl service;

    @Mock
    private MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Mock
    private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("키워드를 삭제한다")
            void it_deletes_member_related_keyword() {
                Member member = mock(Member.class);
                MemberRelatedKeyword mrk = mock(MemberRelatedKeyword.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findById(1L)).thenReturn(Optional.of(mrk));
                when(mrk.getMember()).thenReturn(member);

                service.execute(1L);

                verify(memberRelatedKeywordRepository).delete(mrk);
            }
        }

        @Nested
        @DisplayName("키워드가 존재하지 않을 때")
        class Context_with_not_found_keyword {

            @Test
            @DisplayName("MemberRelatedKeywordNotFoundException을 던진다")
            void it_throws_member_related_keyword_not_found_exception() {
                Member member = mock(Member.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findById(1L)).thenReturn(Optional.empty());

                assertThrows(MemberRelatedKeywordNotFoundException.class, () -> service.execute(1L));
            }
        }

        @Nested
        @DisplayName("본인 키워드가 아닐 때")
        class Context_with_other_member_keyword {

            @Test
            @DisplayName("MemberRelatedKeywordForbiddenException을 던진다")
            void it_throws_member_related_keyword_forbidden_exception() {
                Member member = mock(Member.class);
                Member otherMember = mock(Member.class);
                MemberRelatedKeyword mrk = mock(MemberRelatedKeyword.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findById(1L)).thenReturn(Optional.of(mrk));
                when(mrk.getMember()).thenReturn(otherMember);

                assertThrows(MemberRelatedKeywordForbiddenException.class, () -> service.execute(1L));
            }
        }
    }
}