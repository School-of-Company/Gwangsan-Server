package team.startup.gwangsan.adapter.persistence.notice;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.notice.data.NoticeImageLink;
import team.startup.gwangsan.application.notice.data.NoticeImageResult;
import team.startup.gwangsan.application.notice.port.out.NoticePort;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.notice.exception.NoticeNotFoundException;
import team.startup.gwangsan.domain.notice.model.Notice;
import team.startup.gwangsan.domain.place.entity.Place;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoticePersistenceAdapter implements NoticePort {

    private final NoticeJpaRepository noticeJpaRepository;
    private final NoticeImageJpaRepository noticeImageJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Notice> findById(Long noticeId) {
        return noticeJpaRepository.findById(noticeId).map(NoticePersistenceAdapter::toModel);
    }

    @Override
    public Notice save(Notice notice) {
        if (notice.id() == null) {
            NoticeJpaEntity saved = noticeJpaRepository.save(NoticeJpaEntity.builder()
                    .title(notice.title())
                    .content(notice.content())
                    .place(entityManager.getReference(Place.class, notice.placeId()))
                    .member(notice.authorId() == null ? null : entityManager.getReference(MemberJpaEntity.class, notice.authorId()))
                    .build());
            return toModel(saved);
        }
        NoticeJpaEntity entity = noticeJpaRepository.findById(notice.id())
                .orElseThrow(NoticeNotFoundException::new);
        entity.update(notice.title(), notice.content());
        return toModel(noticeJpaRepository.save(entity));
    }

    @Override
    public void delete(Long noticeId) {
        noticeImageJpaRepository.deleteAllByNoticeId(noticeId);
        noticeJpaRepository.deleteById(noticeId);
    }

    @Override
    public List<Notice> findPage(List<Integer> placeIds, Long lastId, int size) {
        PageRequest page = PageRequest.of(0, size);
        List<NoticeJpaEntity> notices = lastId == null
                ? noticeJpaRepository.findByPlaceIdInOrderByIdDesc(placeIds, page)
                : noticeJpaRepository.findByPlaceIdInAndIdLessThanOrderByIdDesc(placeIds, lastId, page);
        return notices.stream().map(NoticePersistenceAdapter::toModel).toList();
    }

    @Override
    public void addImages(Long noticeId, List<Long> imageIds) {
        NoticeJpaEntity notice = entityManager.getReference(NoticeJpaEntity.class, noticeId);
        noticeImageJpaRepository.saveAll(imageIds.stream()
                .map(imageId -> NoticeImageJpaEntity.builder()
                        .notice(notice)
                        .image(entityManager.getReference(Image.class, imageId))
                        .build())
                .toList());
    }

    @Override
    public void deleteImages(Long noticeId) {
        noticeImageJpaRepository.deleteAllByNoticeId(noticeId);
    }

    @Override
    public List<NoticeImageResult> findImages(Long noticeId) {
        return noticeImageJpaRepository.findAllByNoticeId(noticeId).stream()
                .map(ni -> new NoticeImageResult(ni.getImage().getId(), ni.getImage().getImageUrl()))
                .toList();
    }

    @Override
    public Map<Long, List<NoticeImageLink>> findImageLinks(List<Long> noticeIds) {
        return noticeImageJpaRepository.findAllByNoticeIdIn(noticeIds).stream()
                .collect(Collectors.groupingBy(
                        ni -> ni.getNotice().getId(),
                        Collectors.mapping(ni -> new NoticeImageLink(ni.getId(), ni.getImage().getId(), ni.getImage().getImageUrl()),
                                Collectors.toList())));
    }

    @Override
    public void unlinkImage(Long noticeId, Long imageId) {
        noticeImageJpaRepository.deleteByNoticeIdAndImageId(noticeId, imageId);
    }

    // 연관 엔티티는 프록시의 id 만 읽어 추가 조회를 만들지 않는다.
    private static Notice toModel(NoticeJpaEntity entity) {
        return new Notice(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getPlace().getId(),
                entity.getMember() == null ? null : entity.getMember().getId(),
                entity.getCreatedAt()
        );
    }
}
