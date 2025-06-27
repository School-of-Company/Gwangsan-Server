package team.startup.gwangsan.domain.image.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UploadImageRequest(
        @NotNull MultipartFile file
) {
}
