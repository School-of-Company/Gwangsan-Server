package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.admin.util.ValidatePlaceUtil;
import team.startup.gwangsan.domain.auth.exception.PlaceNotFoundException;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaEntity;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.member.MemberDetailJpaRepository;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMemberRoleServiceImpl 단위 테스트")
class UpdateMemberRoleServiceImplTest {

    @InjectMocks
    private UpdateMemberRoleServiceImpl service;

    @Mock
    private MemberJpaRepository memberRepository;

    @Mock
    private MemberDetailJpaRepository memberDetailRepository;

    @Mock
    private MemberUtil memberUtil;

    @Mock
    private ValidatePlaceUtil validatePlaceUtil;

    @Mock
    private PlaceRepository placeRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("placeId가 null이 아닐 때")
        class Context_with_place_id {

            @Test
            @DisplayName("역할과 소속 장소를 함께 변경한다")
            void it_updates_role_and_place() {
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                MemberJpaEntity target = mock(MemberJpaEntity.class);
                when(target.getId()).thenReturn(2L);
                MemberDetailJpaEntity targetDetail = mock(MemberDetailJpaEntity.class);

                Place place = mock(Place.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));
                when(placeRepository.findById(3)).thenReturn(Optional.of(place));

                service.execute(2L, MemberRole.ROLE_PLACE_ADMIN, 3);

                verify(target).updateMemberRole(MemberRole.ROLE_PLACE_ADMIN);
                verify(targetDetail).updatePlace(place);
            }
        }

        @Nested
        @DisplayName("placeId가 null일 때")
        class Context_without_place_id {

            @Test
            @DisplayName("역할만 변경한다")
            void it_updates_role_only() {
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                MemberJpaEntity target = mock(MemberJpaEntity.class);
                when(target.getId()).thenReturn(2L);
                MemberDetailJpaEntity targetDetail = mock(MemberDetailJpaEntity.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));

                service.execute(2L, MemberRole.ROLE_PLACE_ADMIN, null);

                verify(target).updateMemberRole(MemberRole.ROLE_PLACE_ADMIN);
                verify(targetDetail, never()).updatePlace(any());
                verify(placeRepository, never()).findById(any());
            }
        }

        @Nested
        @DisplayName("대상 회원이 없을 때")
        class Context_with_target_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(2L, MemberRole.ROLE_PLACE_ADMIN, null))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("placeId에 해당하는 장소가 없을 때")
        class Context_with_place_not_found {

            @Test
            @DisplayName("PlaceNotFoundException을 던진다")
            void it_throws_place_not_found_exception() {
                MemberJpaEntity admin = mock(MemberJpaEntity.class);
                when(admin.getId()).thenReturn(1L);
                MemberDetailJpaEntity adminDetail = mock(MemberDetailJpaEntity.class);

                MemberJpaEntity target = mock(MemberJpaEntity.class);
                when(target.getId()).thenReturn(2L);
                MemberDetailJpaEntity targetDetail = mock(MemberDetailJpaEntity.class);

                when(memberUtil.getCurrentMember()).thenReturn(admin);
                when(memberDetailRepository.findById(1L)).thenReturn(Optional.of(adminDetail));
                when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(targetDetail));
                when(placeRepository.findById(99)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(2L, MemberRole.ROLE_PLACE_ADMIN, 99))
                        .isInstanceOf(PlaceNotFoundException.class);
            }
        }
    }
}
