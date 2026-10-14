package team.startup.gwangsan.application.image.port.out;

import team.startup.gwangsan.domain.image.model.Image;

import java.util.List;

public interface LoadOrphanImageCandidatesPort {
    /** 어떤 업무도 참조하지 않는 이미지를 createdAt, id 오름차순으로 limit 개 반환한다. */
    List<Image> findOrphanCandidates(int limit);
}
