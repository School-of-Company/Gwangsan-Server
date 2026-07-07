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
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindMyRelatedKeywordServiceImpl 단위 테스트")
class FindMyRelatedKeywordServiceImplTest {

    @InjectMocks
    private FindMyRelatedKeywordServiceImpl service;

    @Mock
    private MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Mock
    private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("키워드가 있을 때")
        class Context_with_keywords {

            @Test
            @DisplayName("키워드 목록을 반환한다")
            void it_returns_related_keyword_list() {
                Member member = mock(Member.class);

                RelatedKeyword rk1 = mock(RelatedKeyword.class);
                when(rk1.getId()).thenReturn(1L);
                when(rk1.getName()).thenReturn("keyword1");

                RelatedKeyword rk2 = mock(RelatedKeyword.class);
                when(rk2.getId()).thenReturn(2L);
                when(rk2.getName()).thenReturn("keyword2");

                MemberRelatedKeyword mrk1 = mock(MemberRelatedKeyword.class);
                when(mrk1.getRelatedKeyword()).thenReturn(rk1);

                MemberRelatedKeyword mrk2 = mock(MemberRelatedKeyword.class);
                when(mrk2.getRelatedKeyword()).thenReturn(rk2);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(List.of(mrk1, mrk2));

                List<RelatedKeywordResponse> result = service.execute();

                assertEquals(2, result.size());
            }
        }

        @Nested
        @DisplayName("키워드가 없을 때")
        class Context_with_no_keywords {

            @Test
            @DisplayName("빈 리스트를 반환한다")
            void it_returns_empty_list() {
                Member member = mock(Member.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(Collections.emptyList());

                List<RelatedKeywordResponse> result = service.execute();

                assertEquals(0, result.size());
            }
        }
    }
}