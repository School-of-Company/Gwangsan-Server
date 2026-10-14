package team.startup.gwangsan.application.image.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.image.data.ImageSummary;
import team.startup.gwangsan.application.image.port.out.LoadImagePort;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.model.Image;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageQueryService 단위 테스트")
class ImageQueryServiceTest {

    @InjectMocks private ImageQueryService service;
    @Mock private LoadImagePort loadImagePort;

    @Test
    @DisplayName("it_중복 ID 요청도 모두 존재하면 통과한다 (기존 ImageValidateUtil 의미)")
    void it_accepts_duplicate_ids_when_all_exist() {
        List<Long> ids = List.of(1L, 1L);
        when(loadImagePort.findAllByIds(ids)).thenReturn(List.of(new Image(1L, "a", null)));

        assertThat(service.getAllByIds(ids)).containsExactly(new ImageSummary(1L, "a"));
    }

    @Test
    @DisplayName("it_없는 ID 가 있으면 ImageNotFoundException")
    void it_throws_when_any_id_is_missing() {
        List<Long> ids = List.of(1L, 2L);
        when(loadImagePort.findAllByIds(ids)).thenReturn(List.of(new Image(1L, "a", null)));

        assertThatThrownBy(() -> service.getAllByIds(ids)).isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    @DisplayName("it_findAllByIds 는 없는 ID 를 조용히 뺀다")
    void it_skips_missing_ids() {
        List<Long> ids = List.of(1L, 2L);
        when(loadImagePort.findAllByIds(ids)).thenReturn(List.of(new Image(1L, "a", null)));

        assertThat(service.findAllByIds(ids)).containsExactly(new ImageSummary(1L, "a"));
    }
}
