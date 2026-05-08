package team.startup.gwangsan.domain.relatedkeyword.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.presentation.dto.response.RelatedKeywordResponse;
import team.startup.gwangsan.domain.relatedkeyword.repository.RelatedKeywordRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindRelatedKeywordServiceImpl 단위 테스트")
class FindRelatedKeywordServiceImplTest {

    @InjectMocks
    private FindRelatedKeywordServiceImpl service;

    @Mock
    private RelatedKeywordRepository relatedKeywordRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("키워드가 있을 때")
        class Context_with_keywords {

            @Test
            @DisplayName("키워드 목록을 반환한다")
            void it_returns_related_keyword_list() {
                RelatedKeyword rk1 = mock(RelatedKeyword.class);
                when(rk1.getId()).thenReturn(1L);
                when(rk1.getName()).thenReturn("keyword1");

                RelatedKeyword rk2 = mock(RelatedKeyword.class);
                when(rk2.getId()).thenReturn(2L);
                when(rk2.getName()).thenReturn("keyword2");

                when(relatedKeywordRepository.findAll()).thenReturn(List.of(rk1, rk2));

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
                when(relatedKeywordRepository.findAll()).thenReturn(Collections.emptyList());

                List<RelatedKeywordResponse> result = service.execute();

                assertEquals(0, result.size());
            }
        }
    }
}