package team.startup.gwangsan.adapter.integration.feedback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.in.ImageQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackImageBridgeAdapter 단위 테스트")
class FeedbackImageBridgeAdapterTest {

    @Mock private ImageQuery imageQuery;

    @Test
    @DisplayName("존재하는 이미지 ID만 돌려준다")
    void it_returns_existing_image_ids_only() {
        when(imageQuery.findAllByIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new ImageSummary(1L, "url1"), new ImageSummary(2L, "url2")));

        assertThat(new FeedbackImageBridgeAdapter(imageQuery).findExistingImageIds(List.of(1L, 2L, 3L)))
                .containsExactly(1L, 2L);
    }
}
