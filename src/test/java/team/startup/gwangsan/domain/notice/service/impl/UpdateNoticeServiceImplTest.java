package team.startup.gwangsan.domain.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.entity.Notice;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.exception.UnauthorizedNoticeAccessException;
import team.startup.gwangsan.domain.notice.presentation.dto.request.UpdateNoticeRequest;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.notice.repository.NoticeRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateNoticeServiceImpl 단위 테스트")
class UpdateNoticeServiceImplTest {

    @InjectMocks private UpdateNoticeServiceImpl service;

    @Mock private NoticeRepository noticeRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private NoticeImageRepository noticeImageRepository;
    @Mock private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상 수정 요청일 때")
        class Context_with_valid_update_request {

            @Test
            @DisplayName("notice를 업데이트하고 이미지를 저장한다")
            void it_updates_notice_and_saves_images() {
                Member member = mock(Member.class);
                Notice notice = mock(Notice.class);
                Image image = mock(Image.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(member);
                when(member.getId()).thenReturn(1L);
                when(imageRepository.findAllById(List.of(10L))).thenReturn(List.of(image));

                UpdateNoticeRequest request = new UpdateNoticeRequest("새 제목", "새 내용", List.of(10L));
                service.execute(1L, request);

                verify(notice).update("새 제목", "새 내용");
                verify(noticeImageRepository).deleteAllByNotice(notice);
                verify(noticeImageRepository).saveAll(any());
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

                UpdateNoticeRequest request = new UpdateNoticeRequest("제목", "내용", List.of());

                assertThatThrownBy(() -> service.execute(99L, request))
                        .isInstanceOf(NoticeNotFoundException.class);
            }
        }

        @Nested
        @DisplayName("본인이 아닌 사람이 수정 요청할 때")
        class Context_with_not_owner {

            @Test
            @DisplayName("UnauthorizedNoticeAccessException을 던진다")
            void it_throws_unauthorized_notice_access_exception() {
                Member member = mock(Member.class);
                Member noticeOwner = mock(Member.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(noticeOwner);
                when(member.getId()).thenReturn(1L);
                when(noticeOwner.getId()).thenReturn(2L);

                UpdateNoticeRequest request = new UpdateNoticeRequest("제목", "내용", List.of());

                assertThatThrownBy(() -> service.execute(1L, request))
                        .isInstanceOf(UnauthorizedNoticeAccessException.class);
            }
        }

        @Nested
        @DisplayName("이미지 ID 개수와 실제 조회된 이미지 수가 불일치할 때")
        class Context_with_image_count_mismatch {

            @Test
            @DisplayName("ImageNotFoundException을 던진다")
            void it_throws_image_not_found_exception() {
                Member member = mock(Member.class);
                Notice notice = mock(Notice.class);

                when(memberUtil.getCurrentMember()).thenReturn(member);
                when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
                when(notice.getMember()).thenReturn(member);
                when(member.getId()).thenReturn(1L);
                when(imageRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(mock(Image.class)));

                UpdateNoticeRequest request = new UpdateNoticeRequest("제목", "내용", List.of(10L, 20L));

                assertThatThrownBy(() -> service.execute(1L, request))
                        .isInstanceOf(ImageNotFoundException.class);
            }
        }
    }
}