package team.startup.gwangsan.adapter.web.image.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.startup.gwangsan.adapter.web.image.dto.UploadImageResponse;
import team.startup.gwangsan.application.image.data.DeleteImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageResult;
import team.startup.gwangsan.application.image.port.in.DeleteImageUseCase;
import team.startup.gwangsan.application.image.port.in.UploadImageUseCase;
import team.startup.gwangsan.domain.image.exception.ImageUploadFailedException;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image")
public class ImageController {

    private final UploadImageUseCase uploadImageUseCase;
    private final DeleteImageUseCase deleteImageUseCase;

    @PostMapping
    public ResponseEntity<UploadImageResponse> upload(@RequestPart("file") MultipartFile file) {
        UploadImageResult result = uploadImageUseCase.execute(new UploadImageCommand(file.getOriginalFilename(), readContent(file)));
        return ResponseEntity.ok().body(new UploadImageResponse(result.imageId(), result.imageUrl()));
    }

    @DeleteMapping("/{image_id}")
    public ResponseEntity<Void> delete(@PathVariable("image_id") Long imageId) {
        deleteImageUseCase.execute(new DeleteImageCommand(imageId));
        return ResponseEntity.noContent().build();
    }

    // 기존에도 파일 읽기 실패는 S3 업로드 단계에서 IMAGE_UPLOAD_FAILED(500)였다.
    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImageUploadFailedException();
        }
    }
}
