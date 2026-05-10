package team.startup.gwangsan.domain.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.repository.PlaceRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.entity.NoticeImage;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindAllNoticeResponse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindAllNoticeServiceImpl 단위 테스트")
class FindAllNoticeServiceImplTest {

    @InjectMocks private FindAllNoticeServiceImpl service;

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberUtil memberUtil;
    @Mock private PlaceRepository placeRepository;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("일반 ADMIN이고 lastId가 null일 때")
        class Context_with_admin_and_no_last_id {

            @Test
            @DisplayName("findByPlaceOrderByIdDesc를 호출한다")
            void it_calls_find_by_place_order_by_id_desc() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(memberDetail.getPlace()).thenReturn(place);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(noticeRepository.findByPlaceOrderByIdDesc(eq(place), any())).thenReturn(Collections.emptyList());
                when(noticeImageRepository.findAllByNoticeIdIn(any())).thenReturn(Collections.emptyList());

                List<FindAllNoticeResponse> result = service.execute(null, 10);

                assertThat(result).isEmpty();
                verify(noticeRepository).findByPlaceOrderByIdDesc(eq(place), any());
                verify(noticeRepository, never()).findByPlaceAndIdLessThanOrderByIdDesc(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("일반 ADMIN이고 lastId가 있을 때")
        class Context_with_admin_and_last_id {

            @Test
            @DisplayName("findByPlaceAndIdLessThanOrderByIdDesc를 호출한다")
            void it_calls_find_by_place_and_id_less_than_order_by_id_desc() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(memberDetail.getPlace()).thenReturn(place);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(noticeRepository.findByPlaceAndIdLessThanOrderByIdDesc(eq(place), eq(5L), any())).thenReturn(Collections.emptyList());
                when(noticeImageRepository.findAllByNoticeIdIn(any())).thenReturn(Collections.emptyList());

                List<FindAllNoticeResponse> result = service.execute(5L, 10);

                assertThat(result).isEmpty();
                verify(noticeRepository).findByPlaceAndIdLessThanOrderByIdDesc(eq(place), eq(5L), any());
                verify(noticeRepository, never()).findByPlaceOrderByIdDesc(any(), any());
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이고 lastId가 null일 때")
        class Context_with_head_admin_and_no_last_id {

            @Test
            @DisplayName("findByPlaceInOrderByIdDesc를 호출한다")
            void it_calls_find_by_place_in_order_by_id_desc() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);
                Head head = mock(Head.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(memberDetail.getPlace()).thenReturn(place);
                when(member.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);
                when(place.getHead()).thenReturn(head);
                when(placeRepository.findByHead(head)).thenReturn(List.of(place));
                when(noticeRepository.findByPlaceInOrderByIdDesc(any(), any())).thenReturn(Collections.emptyList());
                when(noticeImageRepository.findAllByNoticeIdIn(any())).thenReturn(Collections.emptyList());

                List<FindAllNoticeResponse> result = service.execute(null, 10);

                assertThat(result).isEmpty();
                verify(noticeRepository).findByPlaceInOrderByIdDesc(any(), any());
                verify(noticeRepository, never()).findByPlaceInAndIdLessThanOrderByIdDesc(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이고 lastId가 있을 때")
        class Context_with_head_admin_and_last_id {

            @Test
            @DisplayName("findByPlaceInAndIdLessThanOrderByIdDesc를 호출한다")
            void it_calls_find_by_place_in_and_id_less_than_order_by_id_desc() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);
                Head head = mock(Head.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(memberDetail.getPlace()).thenReturn(place);
                when(member.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);
                when(place.getHead()).thenReturn(head);
                when(placeRepository.findByHead(head)).thenReturn(List.of(place));
                when(noticeRepository.findByPlaceInAndIdLessThanOrderByIdDesc(any(), eq(5L), any())).thenReturn(Collections.emptyList());
                when(noticeImageRepository.findAllByNoticeIdIn(any())).thenReturn(Collections.emptyList());

                List<FindAllNoticeResponse> result = service.execute(5L, 10);

                assertThat(result).isEmpty();
                verify(noticeRepository).findByPlaceInAndIdLessThanOrderByIdDesc(any(), eq(5L), any());
                verify(noticeRepository, never()).findByPlaceInOrderByIdDesc(any(), any());
            }
        }

        @Nested
        @DisplayName("공지 목록과 이미지가 있을 때")
        class Context_with_notices_and_images {

            @Test
            @DisplayName("이미지가 포함된 FindAllNoticeResponse 목록을 반환한다")
            void it_returns_notice_responses_with_images() {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(1L);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);

                Notice notice = mock(Notice.class);
                when(notice.getId()).thenReturn(100L);
                when(notice.getTitle()).thenReturn("공지 제목");
                when(notice.getContent()).thenReturn("공지 내용");
                Member noticeMember = mock(Member.class);
                when(noticeMember.getId()).thenReturn(1L);
                when(notice.getMember()).thenReturn(noticeMember);

                Image image = mock(Image.class);
                when(image.getId()).thenReturn(10L);
                when(image.getImageUrl()).thenReturn("https://s3.example.com/img.png");

                NoticeImage noticeImage = mock(NoticeImage.class);
                when(noticeImage.getNotice()).thenReturn(notice);
                when(noticeImage.getImage()).thenReturn(image);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(memberDetail.getPlace()).thenReturn(place);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(noticeRepository.findByPlaceOrderByIdDesc(eq(place), any())).thenReturn(List.of(notice));
                when(noticeImageRepository.findAllByNoticeIdIn(List.of(100L))).thenReturn(List.of(noticeImage));

                List<FindAllNoticeResponse> result = service.execute(null, 10);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).title()).isEqualTo("공지 제목");
                assertThat(result.get(0).images()).hasSize(1);
                assertThat(result.get(0).isMe()).isTrue();
            }
        }

        @Nested
        @DisplayName("MemberDetail이 없을 때")
        class Context_with_member_detail_not_found {

            @Test
            @DisplayName("NotFoundMemberDetailException을 던진다")
            void it_throws_not_found_member_detail_exception() {
                Member member = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(null, 10))
                        .isInstanceOf(NotFoundMemberDetailException.class);
            }
        }
    }
}