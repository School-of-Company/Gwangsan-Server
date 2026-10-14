package team.startup.gwangsan.adapter.web.image.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import team.startup.gwangsan.application.image.data.DeleteImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageResult;
import team.startup.gwangsan.application.image.port.in.DeleteImageUseCase;
import team.startup.gwangsan.application.image.port.in.UploadImageUseCase;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.exception.InappropriateImageException;
import team.startup.gwangsan.infrastructure.web.GlobalExceptionHandler;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("이미지 API 독립 HTTP 계약")
class ImageControllerStandaloneTest {
    private MockMvc mvc;
    private UploadImageUseCase uploadImageUseCase;
    private DeleteImageUseCase deleteImageUseCase;

    @BeforeEach
    void setUp() {
        uploadImageUseCase = mock(UploadImageUseCase.class);
        deleteImageUseCase = mock(DeleteImageUseCase.class);
        ImageController controller = new ImageController(uploadImageUseCase, deleteImageUseCase);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("이미지 업로드는")
    class Upload {
        @Test
        void it_binds_multipart_file_and_serializes_upload_metadata() throws Exception {
            when(uploadImageUseCase.execute(any())).thenReturn(new UploadImageResult(12L, "cdn.example/image.png"));

            mvc.perform(multipart("/api/image").file(new MockMultipartFile(
                            "file", "image.png", "image/png", new byte[]{1, 2, 3})))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imageId").value(12))
                    .andExpect(jsonPath("$.imageUrl").value("cdn.example/image.png"));

            verify(uploadImageUseCase).execute(argThat(command ->
                    command.fileName().equals("image.png") && Arrays.equals(command.content(), new byte[]{1, 2, 3})));
        }

        @Test
        void it_converts_inappropriate_image_to_bad_request() throws Exception {
            when(uploadImageUseCase.execute(any())).thenThrow(new InappropriateImageException());

            mvc.perform(multipart("/api/image").file(new MockMultipartFile(
                            "file", "image.png", "image/png", new byte[]{1})))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("이미지 삭제는")
    class DeleteImage {
        @Test
        void it_returns_no_content() throws Exception {
            mvc.perform(delete("/api/image/12"))
                    .andExpect(status().isNoContent());

            verify(deleteImageUseCase).execute(new DeleteImageCommand(12L));
        }

        @Test
        void it_converts_missing_image_to_not_found_response() throws Exception {
            doThrow(new ImageNotFoundException()).when(deleteImageUseCase).execute(new DeleteImageCommand(12L));

            mvc.perform(delete("/api/image/12"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
