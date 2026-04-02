package team.startup.gwangsan.domain.member.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindUserInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindUserInfoServiceImpl 단위 테스트")
class FindUserInfoServiceImplTest {

    @InjectMocks
    private FindUserInfoServiceImpl service;

    @Mock private MemberRepository memberRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberRelatedKeywordRepository memberRelatedKeywordRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("회원 정보를 반환한다")
            void it_returns_user_info() {
                Member member = mock(Member.class);
                when(member.getNickname()).thenReturn("테스터이");

                Place place = mock(Place.class);
                when(place.getName()).thenReturn("수완지점");

                MemberDetail detail = mock(MemberDetail.class);
                when(detail.getPlace()).thenReturn(place);
                when(detail.getLight()).thenReturn(3);
                when(detail.getGwangsan()).thenReturn(500);
                when(detail.getDescription()).thenReturn("설명");

                RelatedKeyword keyword = mock(RelatedKeyword.class);
                when(keyword.getName()).thenReturn("Spring");
                MemberRelatedKeyword mrk = mock(MemberRelatedKeyword.class);
                when(mrk.getRelatedKeyword()).thenReturn(keyword);

                when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.of(detail));
                when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(List.of(mrk));

                FindUserInfoResponse response = service.execute(2L);

                assertThat(response.nickname()).isEqualTo("테스터이");
                assertThat(response.placeName()).isEqualTo("수완지점");
                assertThat(response.specialties()).containsExactly("Spring");
            }
        }

        @Nested
        @DisplayName("회원이 없을 때")
        class Context_with_member_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                when(memberRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }

        @Nested
        @DisplayName("MemberDetail이 없을 때")
        class Context_with_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member member = mock(Member.class);
                when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
                when(memberDetailRepository.findById(2L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(2L))
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
