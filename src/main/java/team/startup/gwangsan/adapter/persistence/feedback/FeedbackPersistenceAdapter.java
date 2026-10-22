package team.startup.gwangsan.adapter.persistence.feedback;

import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.application.feedback.data.FeedbackDetail;
import team.startup.gwangsan.application.feedback.data.FeedbackImageLink;
import team.startup.gwangsan.application.feedback.data.FeedbackSummary;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackPort;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements LoadFeedbackPort, SaveFeedbackPort {

    private final FeedbackJpaRepository feedbackJpaRepository;
    private final FeedbackImageJpaRepository feedbackImageJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Feedback> findById(Long feedbackId) {
        return feedbackJpaRepository.findById(feedbackId).map(FeedbackPersistenceAdapter::toDomain);
    }

    @Override
    public List<FeedbackSummary> findMySummaries(Long memberId, Long lastId, int size) {
        List<FeedbackJpaEntity> feedbacks = feedbackJpaRepository.findMyFeedbacks(memberId, lastId, size);
        if (feedbacks.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FeedbackImageLink>> images = loadImageLinks(feedbacks.stream().map(FeedbackJpaEntity::getId).toList());
        return feedbacks.stream()
                .map(f -> new FeedbackSummary(f.getId(), f.getType(), f.getContent(),
                        images.getOrDefault(f.getId(), List.of()), f.getStatus(), f.getAnswer(), f.getAnsweredAt(),
                        f.getCreatedAt()))
                .toList();
    }

    @Override
    public Optional<FeedbackDetail> findDetail(Long feedbackId) {
        return feedbackJpaRepository.findRowById(feedbackId)
                .map(row -> toDetail(row, loadImageLinks(List.of(rowFeedbackId(row))).getOrDefault(rowFeedbackId(row), List.of())));
    }

    @Override
    public Optional<FeedbackAccess> findAccess(Long feedbackId) {
        return feedbackJpaRepository.findAccessRow(feedbackId)
                .map(row -> new FeedbackAccess(row.get(0, FeedbackType.class), row.get(1, Integer.class)));
    }

    @Override
    public List<FeedbackDetail> findAdminDetails(FeedbackStatus status, FeedbackType type, Integer strictPlaceId,
                                                  Integer scopePlaceIdExemptingBug, Long lastId, int size) {
        List<Tuple> rows = feedbackJpaRepository.findAdminRows(status, type, strictPlaceId, scopePlaceIdExemptingBug,
                lastId, size);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<FeedbackImageLink>> images = loadImageLinks(rows.stream().map(FeedbackPersistenceAdapter::rowFeedbackId).toList());
        return rows.stream()
                .map(row -> toDetail(row, images.getOrDefault(rowFeedbackId(row), List.of())))
                .toList();
    }

    @Override
    public Long save(Feedback feedback) {
        FeedbackJpaEntity entity = FeedbackJpaEntity.builder()
                .member(entityManager.getReference(MemberJpaEntity.class, feedback.getMemberId()))
                .type(feedback.getType())
                .content(feedback.getContent())
                .appVersion(feedback.getAppVersion())
                .osType(feedback.getOsType())
                .osVersion(feedback.getOsVersion())
                .deviceModel(feedback.getDeviceModel())
                .status(feedback.getStatus())
                .build();
        return feedbackJpaRepository.save(entity).getId();
    }

    @Override
    public void saveImages(Long feedbackId, List<Long> imageIds) {
        FeedbackJpaEntity feedback = entityManager.getReference(FeedbackJpaEntity.class, feedbackId);
        feedbackImageJpaRepository.saveAll(imageIds.stream()
                .map(imageId -> new FeedbackImageJpaEntity(entityManager.getReference(ImageJpaEntity.class, imageId), feedback))
                .toList());
    }

    @Override
    public void deleteImageLink(Long feedbackId, Long imageId) {
        feedbackImageJpaRepository.deleteByFeedbackIdAndImageId(feedbackId, imageId);
    }

    @Override
    public void saveAnswer(Feedback feedback) {
        FeedbackJpaEntity entity = feedbackJpaRepository.findById(feedback.getId())
                .orElseThrow(NotFoundFeedbackException::new);
        entity.applyAnswer(feedback.getAnswer(),
                entityManager.getReference(MemberJpaEntity.class, feedback.getAnsweredByMemberId()),
                feedback.getAnsweredAt(), feedback.getStatus());
    }

    @Override
    public void changeStatus(Feedback feedback) {
        FeedbackJpaEntity entity = feedbackJpaRepository.findById(feedback.getId())
                .orElseThrow(NotFoundFeedbackException::new);
        entity.updateStatus(feedback.getStatus());
    }

    @Override
    public void reassignMember(Long memberId, Long dummyMemberId) {
        MemberJpaEntity target = entityManager.getReference(MemberJpaEntity.class, memberId);
        MemberJpaEntity dummy = entityManager.getReference(MemberJpaEntity.class, dummyMemberId);
        feedbackJpaRepository.reassignMember(target, dummy);
        feedbackJpaRepository.reassignAnsweredBy(target, dummy);
    }

    private Map<Long, List<FeedbackImageLink>> loadImageLinks(Collection<Long> feedbackIds) {
        return feedbackImageJpaRepository.findAllByFeedbackIdIn(feedbackIds).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getFeedback().getId(),
                        Collectors.mapping(link -> new FeedbackImageLink(link.getImage().getId(), link.getImage().getImageUrl()),
                                Collectors.toList())));
    }

    private static Long rowFeedbackId(Tuple row) {
        return row.get(0, FeedbackJpaEntity.class).getId();
    }

    private static FeedbackDetail toDetail(Tuple row, List<FeedbackImageLink> images) {
        FeedbackJpaEntity f = row.get(0, FeedbackJpaEntity.class);
        String nickname = row.get(1, String.class);
        Integer placeId = row.get(2, Integer.class);
        String placeName = row.get(3, String.class);
        String answeredByNickname = row.get(4, String.class);
        return new FeedbackDetail(f.getId(), f.getMember().getId(), nickname, placeId, placeName, f.getType(),
                f.getContent(), images, f.getAppVersion(), f.getOsType(), f.getOsVersion(), f.getDeviceModel(),
                f.getStatus(), f.getAnswer(), f.getAnsweredAt(), answeredByNickname, f.getCreatedAt());
    }

    private static Feedback toDomain(FeedbackJpaEntity f) {
        MemberJpaEntity answeredBy = f.getAnsweredBy();
        return Feedback.of(f.getId(), f.getMember().getId(), f.getType(), f.getContent(), f.getAppVersion(),
                f.getOsType(), f.getOsVersion(), f.getDeviceModel(), f.getStatus(), f.getAnswer(),
                answeredBy == null ? null : answeredBy.getId(), f.getAnsweredAt(), f.getCreatedAt());
    }
}
