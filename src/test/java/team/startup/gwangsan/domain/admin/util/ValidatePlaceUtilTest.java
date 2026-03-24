package team.startup.gwangsan.domain.admin.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidatePlaceUtil 단위 테스트")
class ValidatePlaceUtilTest {

    @InjectMocks
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private PlaceRepository placeRepository;

    @Nested
    @DisplayName("validateSamePlace() 메서드는")
    class Describe_validateSamePlace {

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN일 때")
        class Context_with_head_admin {

            @Test
            @DisplayName("같은 head 소속 장소이면 통과한다")
            void it_passes_when_same_head() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);

                Place adminPlace = mock(Place.class);
                Head head = mock(Head.class);
                when(adminPlace.getHead()).thenReturn(head);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(adminPlace);

                Place targetPlace = mock(Place.class);
                when(targetPlace.getId()).thenReturn(2);

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getPlace()).thenReturn(targetPlace);

                Place headPlace1 = mock(Place.class);
                when(headPlace1.getId()).thenReturn(1);
                Place headPlace2 = mock(Place.class);
                when(headPlace2.getId()).thenReturn(2);

                when(placeRepository.findByHead(head)).thenReturn(List.of(headPlace1, headPlace2));

                assertThatCode(() -> validatePlaceUtil.validateSamePlace(admin, adminDetail, targetDetail))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("다른 head 소속 장소이면 PlaceMismatchException을 던진다")
            void it_throws_when_different_head() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);

                Place adminPlace = mock(Place.class);
                Head head = mock(Head.class);
                when(adminPlace.getHead()).thenReturn(head);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(adminPlace);

                Place targetPlace = mock(Place.class);
                when(targetPlace.getId()).thenReturn(99);

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getPlace()).thenReturn(targetPlace);

                Place headPlace1 = mock(Place.class);
                when(headPlace1.getId()).thenReturn(1);
                Place headPlace2 = mock(Place.class);
                when(headPlace2.getId()).thenReturn(2);

                when(placeRepository.findByHead(head)).thenReturn(List.of(headPlace1, headPlace2));

                assertThatThrownBy(() -> validatePlaceUtil.validateSamePlace(admin, adminDetail, targetDetail))
                        .isInstanceOf(PlaceMismatchException.class);
            }
        }

        @Nested
        @DisplayName("일반 ROLE_ADMIN일 때")
        class Context_with_regular_admin {

            @Test
            @DisplayName("같은 장소이면 통과한다")
            void it_passes_when_same_place() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);

                Place adminPlace = mock(Place.class);
                when(adminPlace.getId()).thenReturn(1);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(adminPlace);

                Place targetPlace = mock(Place.class);
                when(targetPlace.getId()).thenReturn(1);

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getPlace()).thenReturn(targetPlace);

                assertThatCode(() -> validatePlaceUtil.validateSamePlace(admin, adminDetail, targetDetail))
                        .doesNotThrowAnyException();
            }

            @Test
            @DisplayName("다른 장소이면 PlaceMismatchException을 던진다")
            void it_throws_when_different_place() {
                Member admin = mock(Member.class);
                when(admin.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);

                Place adminPlace = mock(Place.class);
                when(adminPlace.getId()).thenReturn(1);

                MemberDetail adminDetail = mock(MemberDetail.class);
                when(adminDetail.getPlace()).thenReturn(adminPlace);

                Place targetPlace = mock(Place.class);
                when(targetPlace.getId()).thenReturn(2);

                MemberDetail targetDetail = mock(MemberDetail.class);
                when(targetDetail.getPlace()).thenReturn(targetPlace);

                assertThatThrownBy(() -> validatePlaceUtil.validateSamePlace(admin, adminDetail, targetDetail))
                        .isInstanceOf(PlaceMismatchException.class);
            }
        }
    }
}
