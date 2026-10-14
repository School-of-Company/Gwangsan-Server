package team.startup.gwangsan.adapter.persistence.member;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.member.data.AdminScope;
import team.startup.gwangsan.application.member.data.MemberProfile;
import team.startup.gwangsan.application.member.port.out.LoadMemberPort;
import team.startup.gwangsan.application.member.port.out.MemberProfileQueryPort;
import team.startup.gwangsan.application.member.port.out.SaveMemberPort;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.Member;
import team.startup.gwangsan.domain.member.model.MemberDetail;
import team.startup.gwangsan.adapter.persistence.place.PlaceJpaEntity;

import java.util.List;
import java.util.Optional;

/**
 * 회원 저장 adapter. 저장 메서드는 호출 트랜잭션의 managed 엔티티를 다시 찾아 순수 모델 값을 명시적으로 반영한다.
 * DongJpaEntity/PlaceJpaEntity 는 기존 DB association 유지를 위한 저장 모델 참조다(업무 core 에 노출하지 않음).
 */
@Component
@RequiredArgsConstructor
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort, MemberProfileQueryPort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberDetailJpaRepository memberDetailJpaRepository;
    private final WithdrawalRecordJpaRepository withdrawalRecordJpaRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Member> findById(Long memberId) {
        return memberJpaRepository.findById(memberId).map(MemberPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Member> findByNickname(String nickname) {
        return memberJpaRepository.findByNickname(nickname).map(MemberPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Member> findByPhoneNumber(String phoneNumber) {
        return memberJpaRepository.findByPhoneNumber(phoneNumber).map(MemberPersistenceAdapter::toDomain);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return memberJpaRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    @Override
    public Optional<MemberDetail> findDetail(Long memberId) {
        return memberDetailJpaRepository.findById(memberId).map(d -> new MemberDetail(d.getId(),
                d.getDong().getId(), d.getPlace().getId(), d.getGwangsan(), d.getLight(), d.getDescription()));
    }

    @Override
    public boolean isBannedPhoneNumber(String phoneNumber) {
        return withdrawalRecordJpaRepository.existsByPhoneNumberAndBannedIsTrue(phoneNumber);
    }

    @Override
    public Long register(Member member, MemberDetail detail) {
        MemberJpaEntity entity = memberJpaRepository.save(MemberJpaEntity.builder()
                .name(member.getName())
                .nickname(member.getNickname())
                .password(member.getPasswordHash())
                .phoneNumber(member.getPhoneNumber())
                .recommender(member.getRecommenderId() == null ? null
                        : entityManager.getReference(MemberJpaEntity.class, member.getRecommenderId()))
                .role(member.getRole())
                .status(member.getStatus())
                .build());
        memberDetailJpaRepository.save(MemberDetailJpaEntity.builder()
                .member(entity)
                .dong(entityManager.getReference(DongJpaEntity.class, detail.getDongId()))
                .place(entityManager.getReference(PlaceJpaEntity.class, detail.getPlaceId()))
                .gwangsan(detail.getGwangsan())
                .light(detail.getLight())
                .description(detail.getDescription())
                .build());
        return entity.getId();
    }

    @Override
    public void save(Member member) {
        MemberJpaEntity entity = memberJpaRepository.findById(member.getId()).orElseThrow(NotFoundMemberException::new);
        entity.updateNickname(member.getNickname());
        entity.changePassword(member.getPasswordHash());
    }

    @Override
    public void saveDetail(MemberDetail detail) {
        memberDetailJpaRepository.findById(detail.getMemberId())
                .orElseThrow(NotFoundMemberDetailException::new)
                .apply(detail);
    }

    @Override
    public Optional<MemberProfile> findMyProfile(Long memberId) {
        return memberDetailJpaRepository.findByMemberIdWithPlaceHeadDong(memberId)
                .map(d -> profile(d, d.getPlace().getHead().getName(), d.getDong().getName()));
    }

    @Override
    public Optional<MemberProfile> findUserProfile(Long memberId) {
        return memberDetailJpaRepository.findByMemberIdWithMemberAndPlace(memberId).map(d -> profile(d, null, null));
    }

    @Override
    public Optional<AdminScope> findAdminScope(Long memberId) {
        return memberDetailJpaRepository.findById(memberId)
                .map(d -> new AdminScope(d.getPlace().getId(), d.getPlace().getHead().getId()));
    }

    @Override
    public List<MemberProfile> findAll(Integer placeId, Integer headId, String nickname, String placeName) {
        return memberDetailJpaRepository.findAllByRoleAndNicknameAndPlaceName(placeId, headId, nickname, placeName)
                .stream()
                .map(d -> profile(d, null, null))
                .toList();
    }

    private static MemberProfile profile(MemberDetailJpaEntity d, String headName, String dongName) {
        MemberJpaEntity m = d.getMember();
        return new MemberProfile(m.getId(), m.getNickname(), m.getName(), m.getPhoneNumber(), m.getRole(),
                m.getStatus(), m.getJoinedAt(), d.getPlace().getName(), headName, dongName, d.getLight(),
                d.getGwangsan(), d.getDescription());
    }

    private static Member toDomain(MemberJpaEntity e) {
        return new Member(e.getId(), e.getName(), e.getNickname(), e.getPassword(), e.getPhoneNumber(),
                e.getRecommender() == null ? null : e.getRecommender().getId(), e.getRole(), e.getStatus(),
                e.getJoinedAt());
    }
}
