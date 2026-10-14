package team.startup.gwangsan.application.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.image.data.DeleteImageCommand;
import team.startup.gwangsan.application.image.port.in.DeleteImageUseCase;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageStoragePort;
import team.startup.gwangsan.application.image.port.out.LoadImagePort;
import team.startup.gwangsan.domain.image.exception.ImageNotFoundException;
import team.startup.gwangsan.domain.image.model.Image;

@Service
@RequiredArgsConstructor
public class DeleteImageService implements DeleteImageUseCase {

    private final LoadImagePort loadImagePort;
    private final ImageStoragePort imageStoragePort;
    private final DeleteImagePort deleteImagePort;

    @Override
    @Transactional
    public void execute(DeleteImageCommand command) {
        Image image = loadImagePort.findById(command.imageId()).orElseThrow(ImageNotFoundException::new);
        // 기존 순서: 저장소 삭제 요청(완료 대기 안 함) → DB 삭제. 저장소 실패는 DB 삭제를 막지 않는다.
        imageStoragePort.delete(image.imageUrl());
        deleteImagePort.deleteById(image.id());
    }
}
