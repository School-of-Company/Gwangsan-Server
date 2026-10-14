package team.startup.gwangsan.application.image.port.out;

import team.startup.gwangsan.application.image.data.DeleteResult;

import java.util.List;

public interface ImageStoragePort {
    /** 저장 후 공개 URL 을 반환한다. 실패 시 ImageUploadFailedException. */
    String upload(String fileName, byte[] content);

    /** 완료를 기다리지 않는다. 실패는 로그로만 남는다. */
    void delete(String imageUrl);

    /** 모든 삭제 완료를 기다리고 실패 URL 을 모은다. */
    DeleteResult deleteAll(List<String> imageUrls);
}
