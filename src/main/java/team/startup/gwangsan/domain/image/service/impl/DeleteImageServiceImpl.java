package team.startup.gwangsan.domain.image.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.image.service.DeleteImageService;
import team.startup.gwangsan.global.thirdparty.aws.s3.service.S3DeleteService;

@Service
@RequiredArgsConstructor
public class DeleteImageServiceImpl implements DeleteImageService {

    private final ImageRepository imageRepository;
    private final S3DeleteService s3DeleteService;

    @Override
    @Transactional
    public void execute(Long imageId) {
        Image image = imageRepository.findById(imageId).orElseThrow(ImageNotFoundException::new);
        s3DeleteService.execute(image.getImageUrl());
        imageRepository.delete(image);
    }
}
