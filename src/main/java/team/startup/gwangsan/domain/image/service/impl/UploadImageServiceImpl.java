package team.startup.gwangsan.domain.image.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.presentation.dto.response.UploadImageResponse;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.image.service.UploadImageService;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3UploadService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class UploadImageServiceImpl implements UploadImageService {

    private final S3UploadService s3UploadService;
    private final ImageRepository imageRepository;

    @Override
    @Transactional
    public UploadImageResponse execute(MultipartFile file) {
        String imageUrl = uploadFile(file);

        Image image = Image.builder()
                .imageUrl(imageUrl)
                .build();
        imageRepository.save(image);
        return new UploadImageResponse(image.getId(), image.getImageUrl());
    }

    private String uploadFile(MultipartFile file) {
        try {
            return s3UploadService.execute(file.getOriginalFilename(), file.getInputStream()).get();
        } catch (IOException e) {
            // TODO: 커스텀 예외로 변경
            throw new RuntimeException(e);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
