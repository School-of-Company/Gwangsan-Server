package team.startup.gwangsan.domain.image.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.domain.image.presentation.dto.response.UploadImageResponse;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.service.DeleteImageService;
import team.startup.gwangsan.domain.image.service.UploadImageService;
import team.startup.gwangsan.global.exception.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("이미지 API 독립 HTTP 계약")
class ImageControllerStandaloneTest {
    private MockMvc mvc;
    private UploadImageService uploadImageService;
    private DeleteImageService deleteImageService;

    @BeforeEach
    void setUp() {
        uploadImageService = mock(UploadImageService.class);
        deleteImageService = mock(DeleteImageService.class);
        ImageController controller = new ImageController(uploadImageService, deleteImageService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("이미지 업로드는")
    class Upload {
        @Test
        void it_binds_multipart_file_and_serializes_upload_metadata() throws Exception {
            when(uploadImageService.execute(any())).thenReturn(new UploadImageResponse(12L, "cdn.example/image.png"));

            mvc.perform(multipart("/api/image").file(new MockMultipartFile(
                            "file", "image.png", "image/png", new byte[]{1, 2, 3})))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imageId").value(12))
                    .andExpect(jsonPath("$.imageUrl").value("cdn.example/image.png"));

            verify(uploadImageService).execute(argThat(file ->
                    file.getOriginalFilename().equals("image.png") && file.getSize() == 3));
        }
    }

    @Nested
    @DisplayName("이미지 삭제는")
    class DeleteImage {
        @Test
        void it_converts_missing_image_to_not_found_response() throws Exception {
            doThrow(new ImageNotFoundException()).when(deleteImageService).execute(12L);

            mvc.perform(delete("/api/image/12"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
