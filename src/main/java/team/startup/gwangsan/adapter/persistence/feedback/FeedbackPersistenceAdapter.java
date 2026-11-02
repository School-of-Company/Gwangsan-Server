package team.startup.gwangsan.adapter.persistence.feedback;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;
import team.startup.gwangsan.application.feedback.data.*;
import team.startup.gwangsan.application.feedback.port.out.FeedbackPort;
import team.startup.gwangsan.domain.feedback.model.*;
import team.startup.gwangsan.domain.feedback.exception.NotFoundFeedbackException;
import team.startup.gwangsan.adapter.persistence.member.*;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import static team.startup.gwangsan.adapter.persistence.feedback.QFeedbackJpaEntity.feedbackJpaEntity;
import static team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity.memberDetailJpaEntity;
import static team.startup.gwangsan.adapter.persistence.place.QPlaceJpaEntity.placeJpaEntity;

@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements FeedbackPort {
    private final FeedbackJpaRepository repository;
    private final FeedbackImageJpaRepository imageRepository;
    private final EntityManager em;
    private final JPAQueryFactory queries;

    @Override
    public void create(Feedback feedback, List<Long> imageIds) {
        FeedbackJpaEntity entity = repository.save(new FeedbackJpaEntity(feedback, member(feedback.memberId())));
        imageRepository.saveAll(imageIds.stream()
                .map(id -> new FeedbackImageJpaEntity(entity, em.getReference(ImageJpaEntity.class, id))).toList());
    }

    @Override
    public Optional<FeedbackDetail> find(Long id) {
        return details(new BooleanBuilder(feedbackJpaEntity.id.eq(id)), 1).stream().findFirst();
    }

    @Override
    public List<FeedbackDetail> search(FeedbackSearch search) {
        var f = feedbackJpaEntity;
        BooleanBuilder where = new BooleanBuilder();
        if (search.memberId() != null) where.and(f.member.id.eq(search.memberId()));
        if (search.restrictedPlaceId() != null) {
            where.and(f.type.eq(FeedbackType.BUG).or(memberDetailJpaEntity.place.id.eq(search.restrictedPlaceId())));
        }
        if (search.placeId() != null) where.and(memberDetailJpaEntity.place.id.eq(search.placeId()));
        if (search.status() != null) where.and(f.status.eq(search.status()));
        if (search.type() != null) where.and(f.type.eq(search.type()));
        if (search.lastId() != null) where.and(f.id.lt(search.lastId()));
        return details(where, search.size());
    }

    private List<FeedbackDetail> details(BooleanBuilder where, int size) {
        var f = feedbackJpaEntity;
        QMemberJpaEntity author = new QMemberJpaEntity("feedbackAuthor");
        QMemberJpaEntity admin = new QMemberJpaEntity("feedbackAdmin");
        List<Tuple> rows = queries.select(f, author.nickname, placeJpaEntity.id, placeJpaEntity.name, admin.nickname)
                .from(f)
                .join(f.member, author).fetchJoin()
                .leftJoin(f.answeredBy, admin).fetchJoin()
                .leftJoin(memberDetailJpaEntity).on(memberDetailJpaEntity.id.eq(author.id))
                .leftJoin(memberDetailJpaEntity.place, placeJpaEntity)
                .where(where).orderBy(f.id.desc()).limit(size).fetch();
        if (rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream().map(row -> row.get(f).getId()).toList();
        Map<Long, List<FeedbackDetail.FeedbackImage>> images = imageRepository
                .findAllByFeedbackIdInOrderByIdAsc(ids).stream().collect(Collectors.groupingBy(
                        link -> link.getFeedback().getId(), Collectors.mapping(
                                link -> new FeedbackDetail.FeedbackImage(link.getImage().getId(), link.getImage().getImageUrl()),
                                Collectors.toList())));
        return rows.stream().map(row -> {
            Feedback model = row.get(f).toModel();
            return new FeedbackDetail(model, row.get(author.nickname), row.get(placeJpaEntity.id),
                    row.get(placeJpaEntity.name), row.get(admin.nickname), images.getOrDefault(model.id(), List.of()));
        }).toList();
    }

    @Override
    public void update(Feedback feedback) {
        repository.findById(feedback.id()).orElseThrow(NotFoundFeedbackException::new)
                .apply(feedback, feedback.answeredBy() == null ? null : member(feedback.answeredBy()));
    }

    @Override
    public void reassign(Long memberId, Long dummyMemberId) {
        repository.reassignAuthor(member(memberId), member(dummyMemberId));
        repository.reassignAdmin(member(memberId), member(dummyMemberId));
    }

    @Override
    public void unlink(Long feedbackId, Long imageId) {
        imageRepository.unlink(feedbackId, imageId);
    }

    private MemberJpaEntity member(Long id) {
        return em.getReference(MemberJpaEntity.class, id);
    }
}
