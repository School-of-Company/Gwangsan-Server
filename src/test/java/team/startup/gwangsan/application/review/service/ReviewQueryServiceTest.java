package team.startup.gwangsan.application.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.review.data.*;
import team.startup.gwangsan.application.review.port.out.ReviewMemberPort;
import team.startup.gwangsan.application.review.port.out.ReviewPort;
import team.startup.gwangsan.application.review.port.out.ReviewProductPort;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.review.exception.NotFoundReviewException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewQueryService 단위 테스트")
class ReviewQueryServiceTest {

    @InjectMocks private ReviewQueryService service;

    @Mock private ReviewPort reviewPort;
    @Mock private ReviewProductPort reviewProductPort;
    @Mock private ReviewMemberPort reviewMemberPort;

    private static final ReviewImage IMAGE = new ReviewImage(3L, "cdn/3.png");

    @Test
    @DisplayName("작성한 후기는 작성자=나, 대상=상대 이고 상품별 이미지를 한 번에 붙인다")
    void it_maps_written_reviews() {
        when(reviewMemberPort.findNickname(1L)).thenReturn("나");
        when(reviewPort.findWritten(1L)).thenReturn(List.of(
                new ReviewRow(10L, 100L, "좋아요", 80, "상대"), new ReviewRow(9L, 100L, "또", 70, "상대2")));
        when(reviewProductPort.findImages(List.of(100L))).thenReturn(Map.of(100L, List.of(IMAGE)));

        List<ReviewItem> items = service.findWritten(1L);

        assertThat(items).containsExactly(
                new ReviewItem(10L, 100L, "좋아요", 80, "나", "상대", List.of(IMAGE)),
                new ReviewItem(9L, 100L, "또", 70, "나", "상대2", List.of(IMAGE)));
    }

    @Test
    @DisplayName("후기가 없으면 빈 목록을 반환하고 이미지를 조회하지 않는다")
    void it_returns_empty_without_image_lookup() {
        when(reviewMemberPort.findNickname(1L)).thenReturn("나");
        when(reviewPort.findWritten(1L)).thenReturn(List.of());

        assertThat(service.findWritten(1L)).isEmpty();
        verifyNoInteractions(reviewProductPort);
    }

    @Test
    @DisplayName("받은 후기는 작성자=상대, 대상=나 이고 페이지 조건을 그대로 넘긴다(이미지 없으면 빈 목록)")
    void it_maps_received_reviews() {
        ReviewPage page = new ReviewPage(20L, 2);
        when(reviewMemberPort.findNickname(1L)).thenReturn("나");
        when(reviewPort.findReceived(1L, page)).thenReturn(List.of(new ReviewRow(11L, 101L, "고마워요", 90, "작성자")));
        when(reviewProductPort.findImages(List.of(101L))).thenReturn(Map.of());

        assertThat(service.findReceived(1L, page))
                .containsExactly(new ReviewItem(11L, 101L, "고마워요", 90, "작성자", "나", List.of()));
    }

    @Test
    @DisplayName("회원 후기 조회는 회원이 없으면 NotFoundMemberException 을 던진다")
    void it_requires_member() {
        when(reviewMemberPort.findNickname(8L)).thenThrow(new NotFoundMemberException());

        assertThatThrownBy(() -> service.findByMember(8L, ReviewPage.ALL)).isInstanceOf(NotFoundMemberException.class);
        verifyNoInteractions(reviewPort);
    }

    @Test
    @DisplayName("회원 후기 조회는 대상 별명을 그 회원으로 채운다")
    void it_maps_member_reviews() {
        when(reviewMemberPort.findNickname(8L)).thenReturn("대상");
        when(reviewPort.findReceived(8L, ReviewPage.ALL)).thenReturn(List.of(new ReviewRow(12L, 102L, "후기", 70, "작성자")));
        when(reviewProductPort.findImages(List.of(102L))).thenReturn(Map.of());

        assertThat(service.findByMember(8L, ReviewPage.ALL).get(0).targetName()).isEqualTo("대상");
    }

    @Test
    @DisplayName("상세 조회는 상품 이미지를 붙이고, 없으면 NotFoundReviewException 을 던진다")
    void it_finds_detail() {
        when(reviewPort.findDetail(4L)).thenReturn(Optional.of(new ReviewDetail(4L, 7L, "상품", "상세", 88, List.of())));
        when(reviewProductPort.findImages(7L)).thenReturn(List.of(IMAGE));
        when(reviewPort.findDetail(5L)).thenReturn(Optional.empty());

        assertThat(service.findDetail(4L)).isEqualTo(new ReviewDetail(4L, 7L, "상품", "상세", 88, List.of(IMAGE)));
        assertThatThrownBy(() -> service.findDetail(5L)).isInstanceOf(NotFoundReviewException.class);
    }
}
