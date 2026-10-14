package team.startup.gwangsan.application.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.image.data.DeleteUnusedImagesCommand;
import team.startup.gwangsan.application.image.port.in.DeleteUnusedImagesUseCase;
import team.startup.gwangsan.application.image.port.out.DeleteImagePort;
import team.startup.gwangsan.application.image.port.out.ImageReferenceCleanupPort;

/**
 * 커밋 후 비동기 listener 에서 호출된다. 기존 listener 처럼 트랜잭션을 열지 않으며 각 저장소 호출이 자기 트랜잭션을 쓴다.
 * 저장소 객체(S3)는 지우지 않는다(기존 동작).
 */
@Service
@RequiredArgsConstructor
public class DeleteUnusedImagesService implements DeleteUnusedImagesUseCase {

    private final ImageReferenceCleanupPort imageReferenceCleanupPort;
    private final DeleteImagePort deleteImagePort;

    @Override
    public void execute(DeleteUnusedImagesCommand command) {
        for (Long imageId : command.imageIds()) {
            imageReferenceCleanupPort.unlink(command.ownerType(), command.sourceId(), imageId);
            deleteImagePort.deleteById(imageId);
        }
    }
}
