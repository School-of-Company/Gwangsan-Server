package team.startup.gwangsan.application.relatedkeyword.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.relatedkeyword.data.DeleteMemberRelatedKeywordCommand;
import team.startup.gwangsan.application.relatedkeyword.data.RelatedKeywordResult;
import team.startup.gwangsan.application.relatedkeyword.port.out.RelatedKeywordPort;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordForbiddenException;
import team.startup.gwangsan.domain.relatedkeyword.exception.MemberRelatedKeywordNotFoundException;
import team.startup.gwangsan.domain.relatedkeyword.model.MemberKeyword;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("관심 키워드 유스케이스 단위 테스트")
class RelatedKeywordServicesTest {

    private final RelatedKeywordPort port = mock(RelatedKeywordPort.class);

    @Nested
    @DisplayName("FindRelatedKeywordsService.query() 는")
    class Describe_find_all {

        @Test
        @DisplayName("저장 순서 그대로 전체 키워드를 반환한다")
        void it_returns_related_keyword_list() {
            List<RelatedKeywordResult> keywords = List.of(new RelatedKeywordResult(2L, "요리"), new RelatedKeywordResult(1L, "청소"));
            when(port.findAll()).thenReturn(keywords);

            assertThat(new FindRelatedKeywordsService(port).query()).containsExactlyElementsOf(keywords);
        }

        @Test
        @DisplayName("키워드가 없으면 빈 리스트를 반환한다")
        void it_returns_empty_list() {
            when(port.findAll()).thenReturn(List.of());

            assertThat(new FindRelatedKeywordsService(port).query()).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindMemberRelatedKeywordsService.query() 는")
    class Describe_find_member {

        @Test
        @DisplayName("실행자 회원 ID 의 키워드(키워드 ID·이름)를 반환한다")
        void it_returns_member_keywords() {
            List<RelatedKeywordResult> keywords = List.of(new RelatedKeywordResult(5L, "수리"));
            when(port.findAllByMemberId(7L)).thenReturn(keywords);

            assertThat(new FindMemberRelatedKeywordsService(port).query(7L)).isEqualTo(keywords);
        }
    }

    @Nested
    @DisplayName("DeleteMemberRelatedKeywordService.execute() 는")
    class Describe_delete {

        private final DeleteMemberRelatedKeywordService service = new DeleteMemberRelatedKeywordService(port);

        @Test
        @DisplayName("본인 매핑이면 삭제한다")
        void it_deletes_member_related_keyword() {
            when(port.findMemberKeyword(1L)).thenReturn(Optional.of(new MemberKeyword(1L, 7L)));

            service.execute(new DeleteMemberRelatedKeywordCommand(7L, 1L));

            verify(port).deleteMemberKeyword(1L);
        }

        @Test
        @DisplayName("매핑이 없으면 MemberRelatedKeywordNotFoundException 을 던진다")
        void it_throws_member_related_keyword_not_found_exception() {
            when(port.findMemberKeyword(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new DeleteMemberRelatedKeywordCommand(7L, 1L)))
                    .isInstanceOf(MemberRelatedKeywordNotFoundException.class);
            verify(port, never()).deleteMemberKeyword(anyLong());
        }

        @Test
        @DisplayName("타인 매핑이면 MemberRelatedKeywordForbiddenException 을 던지고 삭제하지 않는다")
        void it_throws_member_related_keyword_forbidden_exception() {
            when(port.findMemberKeyword(1L)).thenReturn(Optional.of(new MemberKeyword(1L, 8L)));

            assertThatThrownBy(() -> service.execute(new DeleteMemberRelatedKeywordCommand(7L, 1L)))
                    .isInstanceOf(MemberRelatedKeywordForbiddenException.class);
            verify(port, never()).deleteMemberKeyword(anyLong());
        }
    }

    @Nested
    @DisplayName("DeleteAllMemberRelatedKeywordsService.execute() 는")
    class Describe_delete_all {

        @Test
        @DisplayName("회원 키워드 매핑을 bulk 삭제 Port 로 지운다")
        void it_bulk_deletes_member_keywords() {
            new DeleteAllMemberRelatedKeywordsService(port).execute(7L);

            verify(port).bulkDeleteAllByMemberId(7L);
            verifyNoMoreInteractions(port);
        }
    }
}
