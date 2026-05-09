package team.startup.gwangsan.domain.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeForbiddenException;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNoticeServiceImpl 단위 테스트")
class DeleteNoticeServiceImplTest {

    @InjectMocks private DeleteNoticeServiceImpl service;

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("작성자 본인이 삭제 요청할 때")
        class Context_with_owner_deletes {

            @Test
            @DisplayName("공지를 삭제한다")
            void it_deletes_notice() {
                Member member = mock(Member.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(member);

                service.execute(1L);

                verify(noticeImageRepository).deleteAllByNotice(notice);
                verify(noticeRepository).delete(notice);
            }
        }

        @Nested
        @DisplayName("ROLE_HEAD_ADMIN이 타인의 공지를 삭제 요청할 때")
        class Context_with_head_admin_deletes_others_notice {

            @Test
            @DisplayName("공지를 삭제한다")
            void it_deletes_notice_by_head_admin() {
                Member headAdmin = mock(Member.class);
                Member noticeOwner = mock(Member.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(headAdmin);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(noticeOwner);
                when(headAdmin.getRole()).thenReturn(MemberRole.ROLE_HEAD_ADMIN);

                service.execute(1L);

                verify(noticeImageRepository).deleteAllByNotice(notice);
                verify(noticeRepository).delete(notice);
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
        @DisplayName("본인 글도 아니고 HEAD_ADMIN도 아닐 때")
        class Context_with_not_owner_and_not_head_admin {

            @Test
            @DisplayName("NoticeForbiddenException을 던진다")
            void it_throws_notice_forbidden_exception() {
                Member member = mock(Member.class);
                Member noticeOwner = mock(Member.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(noticeOwner);
                when(member.getRole()).thenReturn(MemberRole.ROLE_PLACE_ADMIN);

                assertThatThrownBy(() -> service.execute(1L))
                        .isInstanceOf(NoticeForbiddenException.class);
            }
        }
    }
}