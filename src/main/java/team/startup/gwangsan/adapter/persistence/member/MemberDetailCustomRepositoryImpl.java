package team.startup.gwangsan.adapter.persistence.member;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import team.startup.gwangsan.adapter.persistence.member.QMemberJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.QMemberDetailJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.QHeadJpaEntity;
import team.startup.gwangsan.adapter.persistence.place.QPlaceJpaEntity;

@Repository
@RequiredArgsConstructor
public class MemberDetailCustomRepositoryImpl implements MemberDetailCustomRepository {
    private static final QHeadJpaEntity head = QHeadJpaEntity.headJpaEntity;
    private static final QPlaceJpaEntity place = QPlaceJpaEntity.placeJpaEntity;

    private static final QMemberJpaEntity member = QMemberJpaEntity.memberJpaEntity;
    private static final QMemberDetailJpaEntity memberDetail = QMemberDetailJpaEntity.memberDetailJpaEntity;

    private final JPAQueryFactory queryFactory;

    @Override
    public PlaceJpaEntity findPlaceByMemberId(Long id) {
        return Optional.ofNullable(
                queryFactory
                        .select(memberDetail.place)
                        .from(memberDetail)
                        .where(memberDetail.id.eq(id))
                        .fetchOne())
                .orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public Map<Long, String> findPlaceNameMapByMemberIds(Set<Long> memberIds) {
        return queryFactory
                .select(Projections.tuple(
                        memberDetail.member.id,
                        memberDetail.place.name
                ))
                .from(memberDetail)
                .where(memberDetail.member.id.in(memberIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(memberDetail.member.id),
                        tuple -> tuple.get(memberDetail.place.name),
                        (prev, next) -> prev
                ));
    }

    @Override
    public List<MemberDetailJpaEntity> findAllByRoleAndNicknameAndPlaceName(Integer placeId, Integer headId, String nickname, String placeName) {
        return queryFactory
                .selectFrom(memberDetail)
                .join(memberDetail.member, member).fetchJoin()
                .join(memberDetail.place, place).fetchJoin()
                .join(place.head, head).fetchJoin()
                .where(
                        roleCondition(placeId, headId),
                        nicknameEq(nickname),
                        placeNameEq(placeName)
                )
                .fetch();
    }

    @Override
    public MemberDetailJpaEntity findByMemberIdWithMember(Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(memberDetail)
                        .join(memberDetail.member).fetchJoin()
                        .where(memberDetail.member.id.eq(memberId))
                        .fetchOne())
                .orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public MemberDetailJpaEntity findByPhoneNumberWithMember(String phoneNumber) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(memberDetail)
                        .join(memberDetail.member).fetchJoin()
                        .where(memberDetail.member.phoneNumber.eq(phoneNumber))
                        .fetchOne())
                .orElseThrow(NotFoundMemberException::new);
    }

    @Override
    public Optional<MemberDetailJpaEntity> findByMemberIdWithPlaceHeadDong(Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(memberDetail)
                        .join(memberDetail.member).fetchJoin()
                        .join(memberDetail.place, place).fetchJoin()
                        .join(place.head, head).fetchJoin()
                        .join(memberDetail.dong).fetchJoin()
                        .where(memberDetail.member.id.eq(memberId))
                        .fetchOne()
        );
    }

    @Override
    public Optional<MemberDetailJpaEntity> findByMemberIdWithMemberAndPlace(Long memberId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(memberDetail)
                        .join(memberDetail.member, member).fetchJoin()
                        .join(memberDetail.place, place).fetchJoin()
                        .where(memberDetail.member.id.eq(memberId))
                        .fetchOne()
        );
    }


    private BooleanExpression roleCondition(Integer placeId, Integer headId) {
        if (placeId != null) {
            return place.id.eq(placeId);
        } else if (headId != null) {
            return place.head.id.eq(headId);
        } else {
            return null;
        }
    }

    private BooleanExpression nicknameEq(String nickname) {
        return nickname != null ? member.nickname.eq(nickname) : null;
    }

    private BooleanExpression placeNameEq(String placeName) {
        return placeName != null ? place.name.eq(placeName) : null;
    }

    private BooleanExpression placeIdEq(Integer placeId) {
        return placeId != null ? place.id.eq(placeId) : null;
    }

    private BooleanExpression headIdEq(Integer headId) {
        return headId != null ? head.id.eq(headId) : null;
    }
}
