package team.startup.gwangsan.domain.image.service;

import org.springframework.web.multipart.MultipartFile;
import team.startup.gwangsan.domain.image.presentation.dto.response.UploadImageResponse;

public interface UploadImageService {
    UploadImageResponse execute(MultipartFile file);
}
