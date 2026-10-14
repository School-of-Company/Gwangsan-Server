package team.startup.gwangsan.application.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.image.data.UploadImageCommand;
import team.startup.gwangsan.application.image.data.UploadImageResult;
import team.startup.gwangsan.application.image.port.in.UploadImageUseCase;
import team.startup.gwangsan.application.image.port.out.ImageModerationPort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.SaveImagePort;
import team.startup.gwangsan.domain.image.exception.InappropriateImageException;
import team.startup.gwangsan.domain.image.model.Image;

@Service
@RequiredArgsConstructor
public class UploadImageService implements UploadImageUseCase {

    private final ImageModerationPort imageModerationPort;
    private final ImageStoragePort imageStoragePort;
    private final SaveImagePort saveImagePort;

    @Override
    @Transactional
    public UploadImageResult execute(UploadImageCommand command) {
        if (imageModerationPort.isNsfw(command.fileName(), command.content())) {
            throw new InappropriateImageException();
        }

        String imageUrl = imageStoragePort.upload(command.fileName(), command.content());

        Image saved = saveImagePort.save(Image.create(imageUrl));
        return new UploadImageResult(saved.id(), saved.imageUrl());
    }
}
