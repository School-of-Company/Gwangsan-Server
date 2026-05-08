package team.startup.gwangsan.domain.notice.service.impl;

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
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.presentation.dto.response.FindNoticeResponse;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.domain.place.entity.Head;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindNoticeServiceImpl 단위 테스트")
class FindNoticeServiceImplTest {

    @InjectMocks private FindNoticeServiceImpl service;

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private MemberDetailRepository memberDetailRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("일반 ADMIN이고 자신의 Place 공지를 조회할 때")
        class Context_with_admin_same_place {

            @Test
            @DisplayName("FindNoticeResponse를 반환한다")
            void it_returns_find_notice_response() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place place = mock(Place.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(memberDetail.getPlace()).thenReturn(place);
                when(notice.getPlace()).thenReturn(place);
                when(noticeImageRepository.findAllByNotice(notice)).thenReturn(Collections.emptyList());
                when(notice.getMember()).thenReturn(member);
                when(member.getId()).thenReturn(1L);
                when(notice.getId()).thenReturn(1L);
                when(notice.getTitle()).thenReturn("제목");
                when(notice.getContent()).thenReturn("내용");
                when(place.getName()).thenReturn("광산지부");

                FindNoticeResponse response = service.execute(1L);

                assertThat(response).isNotNull();
                assertThat(response.title()).isEqualTo("제목");
            }
        }

        @Nested
        @DisplayName("일반 ADMIN이고 다른 Place 공지를 조회할 때")
        class Context_with_admin_different_place {

            @Test
            @DisplayName("NoticeNotFoundException을 던진다")
            void it_throws_notice_not_found_exception() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place myPlace = mock(Place.class);
                Place otherPlace = mock(Place.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);
                when(memberDetail.getPlace()).thenReturn(myPlace);
                when(notice.getPlace()).thenReturn(otherPlace);

                assertThatThrownBy(() -> service.execute(1L))
                        .isInstanceOf(NoticeNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이고 같은 HEAD 산하 공지를 조회할 때")
        class Context_with_head_admin_same_head {

            @Test
            @DisplayName("FindNoticeResponse를 반환한다")
            void it_returns_find_notice_response() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place myPlace = mock(Place.class);
                Place noticePlace = mock(Place.class);
                Head sameHead = mock(Head.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(member.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);
                when(memberDetail.getPlace()).thenReturn(myPlace);
                when(myPlace.getHead()).thenReturn(sameHead);
                when(notice.getPlace()).thenReturn(noticePlace);
                when(noticePlace.getHead()).thenReturn(sameHead);
                when(noticeImageRepository.findAllByNotice(notice)).thenReturn(Collections.emptyList());
                when(notice.getMember()).thenReturn(member);
                when(member.getId()).thenReturn(1L);
                when(notice.getId()).thenReturn(1L);
                when(notice.getTitle()).thenReturn("제목");
                when(notice.getContent()).thenReturn("내용");
                when(noticePlace.getName()).thenReturn("광산지부");

                FindNoticeResponse response = service.execute(1L);

                assertThat(response).isNotNull();
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이고 다른 HEAD 산하 공지를 조회할 때")
        class Context_with_head_admin_different_head {

            @Test
            @DisplayName("NoticeNotFoundException을 던진다")
            void it_throws_notice_not_found_exception() {
                Member member = mock(Member.class);
                MemberDetail memberDetail = mock(MemberDetail.class);
                Place myPlace = mock(Place.class);
                Place noticePlace = mock(Place.class);
                Head myHead = mock(Head.class);
                Head otherHead = mock(Head.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.of(memberDetail));
                when(member.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);
                when(memberDetail.getPlace()).thenReturn(myPlace);
                when(myPlace.getHead()).thenReturn(myHead);
                when(notice.getPlace()).thenReturn(noticePlace);
                when(noticePlace.getHead()).thenReturn(otherHead);

                assertThatThrownBy(() -> service.execute(1L))
                        .isInstanceOf(NoticeNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("공지가 존재하지 않을 때")
        class Context_with_notice_not_found {

            @Test
            @DisplayName("NoticeNotFoundException을 던진다")
            void it_throws_notice_not_found_exception() {
                Member member = mock(Member.class);
                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(99L))
                        .isInstanceOf(NoticeNotFoundException.class);
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

                Notice notice = mock(Notice.class);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(memberDetailRepository.findByMember(member)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.execute(1L))
                        .isInstanceOf(NotFoundMemberDetailException.class);
            }
        }
    }
}