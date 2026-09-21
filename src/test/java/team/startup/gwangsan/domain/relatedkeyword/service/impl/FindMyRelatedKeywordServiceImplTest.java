package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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

                RelatedKeyword rk1 = RelatedKeyword.builder().name("keyword1").build();
                ReflectionTestUtils.setField(rk1, "id", 1L);
                RelatedKeyword rk2 = RelatedKeyword.builder().name("keyword2").build();
                ReflectionTestUtils.setField(rk2, "id", 2L);

                MemberRelatedKeyword mrk1 = MemberRelatedKeyword.builder()
                        .member(member)
                        .relatedKeyword(rk1)
                        .build();
                MemberRelatedKeyword mrk2 = MemberRelatedKeyword.builder()
                        .member(member)
                        .relatedKeyword(rk2)
                        .build();

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(List.of(mrk1, mrk2));

                List<RelatedKeywordResponse> result = service.execute();

                assertEquals(List.of(
                        new RelatedKeywordResponse(1L, "keyword1"),
                        new RelatedKeywordResponse(2L, "keyword2")
                ), result);
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
