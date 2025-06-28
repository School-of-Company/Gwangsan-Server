package team.startup.gwangsan.domain.image.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.startup.gwangsan.domain.image.presentation.dto.response.UploadImageResponse;
import team.startup.gwangsan.domain.image.service.DeleteImageService;
import team.startup.gwangsan.domain.image.service.UploadImageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image")
public class ImageController {

    private final UploadImageService imageUploadService;
    private final DeleteImageService imageDeleteService;

    @PostMapping
    public ResponseEntity<UploadImageResponse> upload(@RequestBody @Valid MultipartFile file) {
        UploadImageResponse response = imageUploadService.execute(file);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{image_id}")
    public ResponseEntity<Void> delete(@PathVariable("image_id") Long imageId) {
        imageDeleteService.execute(imageId);
        return ResponseEntity.noContent().build();
    }

}
