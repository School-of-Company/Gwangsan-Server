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
import team.startup.gwangsan.domain.member.peresentation.dto.response.FindMyInfoResponse;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.dong.entity.Dong;
import team.startup.gwangsan.domain.relatedkeyword.entity.MemberRelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.entity.RelatedKeyword;
import team.startup.gwangsan.domain.relatedkeyword.repository.MemberRelatedKeywordRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindMyInfoServiceImpl 단위 테스트")
class FindMyInfoServiceImplTest {

    @InjectMocks
    private FindMyInfoServiceImpl service;

    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberRelatedKeywordRepository memberRelatedKeywordRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 요청일 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("내 정보를 반환한다")
            void it_returns_my_info() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(member.getNickname()).thenReturn("테스터일");

                Head head = mock(Head.class);
                when(head.getName()).thenReturn("광산구");

                Place place = mock(Place.class);
                when(place.getName()).thenReturn("수완지점");
                when(place.getHead()).thenReturn(head);

                Dong dong = mock(Dong.class);
                when(dong.getName()).thenReturn("수완동");

                MemberDetail detail = mock(MemberDetail.class);
                when(detail.getPlace()).thenReturn(place);
                when(detail.getDong()).thenReturn(dong);
                when(detail.getLight()).thenReturn(5);
                when(detail.getGwangsan()).thenReturn(1000);
                when(detail.getDescription()).thenReturn("자기소개");

                RelatedKeyword keyword = mock(RelatedKeyword.class);
                when(keyword.getName()).thenReturn("Java");
                MemberRelatedKeyword mrk = mock(MemberRelatedKeyword.class);
                when(mrk.getRelatedKeyword()).thenReturn(keyword);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMemberIdWithPlaceHeadDong(1L)).thenReturn(Optional.of(detail));
                when(memberRelatedKeywordRepository.findAllByMember(member)).thenReturn(List.of(mrk));

                FindMyInfoResponse response = service.execute();

                assertThat(response.nickname()).isEqualTo("테스터일");
                assertThat(response.placeName()).isEqualTo("수완지점");
                assertThat(response.specialties()).containsExactly("Java");
            }
        }

        @Nested
        @DisplayName("MemberDetail이 없을 때")
        class Context_with_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberException을 던진다")
            void it_throws_not_found_member_exception() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMemberIdWithPlaceHeadDong(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute())
                        .isInstanceOf(NotFoundMemberException.class);
            }
        }
    }
}
