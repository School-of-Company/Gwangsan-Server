package team.startup.gwangsan.adapter.integration.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import team.startup.gwangsan.application.place.data.PlaceResult;
import team.startup.gwangsan.application.place.port.in.PlaceQuery;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("회원 지점 검증 어댑터")
class MemberPlaceAdapterTest {
    private final PlaceQuery query = mock(PlaceQuery.class);
    private final MemberPlaceAdapter adapter = new MemberPlaceAdapter(query);

    @Nested
    @DisplayName("지점 검증은")
    class Describe_validate {
        @Test
        @DisplayName("it_앱 선택지 외 지점도 존재하면 허용한다")
        void it_accepts_existing_place_outside_app_choices() {
            when(query.findById(7)).thenReturn(Optional.of(new PlaceResult(7, "지점", 1)));
            assertThatCode(() -> adapter.validate(7)).doesNotThrowAnyException();
            verify(query).findById(7);
        }

        @Test
        @DisplayName("it_없는 지점은 기존 지점 예외로 거절한다")
        void it_rejects_unknown_place() {
            when(query.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> adapter.validate(999)).isInstanceOf(PlaceNotFoundException.class);
        }
    }
}
