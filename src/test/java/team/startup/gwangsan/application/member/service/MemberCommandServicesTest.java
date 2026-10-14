package team.startup.gwangsan.application.member.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.adapter.integration.member.MemberKeywordAdapter;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordPersistenceAdapter;
import team.startup.gwangsan.adapter.persistence.member.*;
import team.startup.gwangsan.application.member.data.MemberAccount;
import team.startup.gwangsan.application.member.data.RegisterMemberCommand;
import team.startup.gwangsan.application.member.data.UpdateMyInfoCommand;
import team.startup.gwangsan.application.relatedkeyword.service.FindMemberRelatedKeywordsService;
import team.startup.gwangsan.application.relatedkeyword.service.MemberKeywordCommandService;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.adapter.persistence.dong.DongJpaEntity;
import team.startup.gwangsan.domain.member.exception.DuplicateMemberNicknameException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.model.MemberRole;
import team.startup.gwangsan.domain.member.model.MemberStatus;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.MemberRelatedKeywordJpaEntity;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordJpaEntity;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.MemberRelatedKeywordJpaRepository;
import team.startup.gwangsan.adapter.persistence.relatedkeyword.RelatedKeywordJpaRepository;
import team.startup.gwangsan.global.exception.ErrorCode;
import team.startup.gwangsan.global.exception.GlobalException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 회원 변경 서비스. 실제 persistence·keyword adapter 를 거쳐 순수 모델 변경이 managed 엔티티에 명시적으로 반영되는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("회원 변경 서비스 단위 테스트")
class MemberCommandServicesTest {

    private static final ActorContext ACTOR = new ActorContext(1L, ActorContext.Role.ROLE_USER);

    @Mock private MemberJpaRepository memberJpaRepository;
    @Mock private MemberDetailJpaRepository memberDetailJpaRepository;
    @Mock private WithdrawalRecordJpaRepository withdrawalRecordJpaRepository;
    @Mock private RelatedKeywordJpaRepository relatedKeywordRepository;
    @Mock private MemberRelatedKeywordJpaRepository memberRelatedKeywordRepository;
    @Mock private EntityManager entityManager;

    private MemberPersistenceAdapter persistence;
    private MemberKeywordAdapter keywords;

    @BeforeEach
    void setUp() {
        persistence = new MemberPersistenceAdapter(memberJpaRepository, memberDetailJpaRepository,
                withdrawalRecordJpaRepository, entityManager);
        RelatedKeywordPersistenceAdapter keywordPersistence = new RelatedKeywordPersistenceAdapter(
                relatedKeywordRepository, memberRelatedKeywordRepository, entityManager);
        keywords = new MemberKeywordAdapter(
                new FindMemberRelatedKeywordsService(keywordPersistence), new MemberKeywordCommandService(keywordPersistence));
    }

    private MemberJpaEntity member(Long id, String nickname) {
        MemberJpaEntity member = MemberJpaEntity.builder().name("이름").nickname(nickname).password("encoded")
                .phoneNumber("01012345678").role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private MemberDetailJpaEntity detail(MemberJpaEntity member, int gwangsan) {
        DongJpaEntity dong = mock(DongJpaEntity.class);
        Place place = mock(Place.class);
        MemberDetailJpaEntity detail = MemberDetailJpaEntity.builder().member(member).dong(dong).place(place)
                .gwangsan(gwangsan).light(1).description("기존 설명").build();
        ReflectionTestUtils.setField(detail, "id", member.getId());
        return detail;
    }

    @Nested
    @DisplayName("UpdateMyInfoService.execute() 는")
    class Describe_update_my_info {

        private UpdateMyInfoService service() {
            return new UpdateMyInfoService(persistence, persistence, keywords);
        }

        private MemberDetailJpaEntity stubMember(MemberJpaEntity member) {
            MemberDetailJpaEntity detail = detail(member, 0);
            when(memberJpaRepository.findById(1L)).thenReturn(Optional.of(member));
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.of(detail));
            when(entityManager.getReference(MemberJpaEntity.class, 1L)).thenReturn(member);
            return detail;
        }

        @Test
        @DisplayName("닉네임이 같으면 중복 체크 없이 설명과 특기를 갱신한다")
        void it_updates_without_duplicate_check() {
            MemberJpaEntity member = member(1L, "테스터일");
            MemberDetailJpaEntity detail = stubMember(member);
            RelatedKeywordJpaEntity java = RelatedKeywordJpaEntity.builder().name("Java").build();
            when(relatedKeywordRepository.findByName("Java")).thenReturn(Optional.of(java));

            service().execute(new UpdateMyInfoCommand(ACTOR, "테스터일", "새 설명", List.of("Java")));

            verify(memberJpaRepository, never()).existsByNickname(any());
            assertThat(member.getNickname()).isEqualTo("테스터일");
            assertThat(detail.getDescription()).isEqualTo("새 설명");
            verify(memberRelatedKeywordRepository).deleteAllByMember(member);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<MemberRelatedKeywordJpaEntity>> saved = ArgumentCaptor.forClass(List.class);
            verify(memberRelatedKeywordRepository).saveAll(saved.capture());
            assertThat(saved.getValue()).extracting(MemberRelatedKeywordJpaEntity::getRelatedKeyword).containsExactly(java);
        }

        @Test
        @DisplayName("존재하지 않는 특기는 새 키워드로 만든다")
        void it_creates_missing_keyword() {
            stubMember(member(1L, "테스터일"));
            when(relatedKeywordRepository.findByName("NewSkill")).thenReturn(Optional.empty());
            when(relatedKeywordRepository.save(any(RelatedKeywordJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

            service().execute(new UpdateMyInfoCommand(ACTOR, "테스터일", "새 설명", List.of("NewSkill")));

            ArgumentCaptor<RelatedKeywordJpaEntity> created = ArgumentCaptor.forClass(RelatedKeywordJpaEntity.class);
            verify(relatedKeywordRepository).save(created.capture());
            assertThat(created.getValue().getName()).isEqualTo("NewSkill");
            verify(memberRelatedKeywordRepository).saveAll(any());
        }

        @Test
        @DisplayName("닉네임이 바뀌면 중복 체크 후 managed 엔티티의 닉네임을 바꾼다")
        void it_changes_nickname_after_duplicate_check() {
            MemberJpaEntity member = member(1L, "기존닉네임");
            stubMember(member);
            when(memberJpaRepository.existsByNickname("새닉네임")).thenReturn(false);

            service().execute(new UpdateMyInfoCommand(ACTOR, "새닉네임", "새 설명", List.of()));

            assertThat(member.getNickname()).isEqualTo("새닉네임");
            assertThat(member.getPassword()).isEqualTo("encoded");
        }

        @Test
        @DisplayName("바꾸려는 닉네임이 이미 있으면 기존과 같은 DUPLICATE_NICKNAME 오류를 던지고 아무것도 바꾸지 않는다")
        void it_throws_duplicate_nickname_exception() {
            MemberJpaEntity member = member(1L, "기존닉네임");
            when(memberJpaRepository.findById(1L)).thenReturn(Optional.of(member));
            when(memberJpaRepository.existsByNickname("중복닉네임")).thenReturn(true);

            assertThatThrownBy(() -> service().execute(new UpdateMyInfoCommand(ACTOR, "중복닉네임", "설명", List.of())))
                    .isInstanceOf(DuplicateMemberNicknameException.class)
                    .extracting(e -> ((GlobalException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
            assertThat(member.getNickname()).isEqualTo("기존닉네임");
            verifyNoInteractions(memberRelatedKeywordRepository);
        }

        @Test
        @DisplayName("상세 정보가 없으면 NotFoundMemberException을 던진다")
        void it_throws_not_found_member_exception() {
            when(memberJpaRepository.findById(1L)).thenReturn(Optional.of(member(1L, "테스터일")));
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().execute(new UpdateMyInfoCommand(ACTOR, "테스터일", "설명", List.of())))
                    .isInstanceOf(NotFoundMemberException.class);
        }
    }

    @Nested
    @DisplayName("ChangeGwangsanService 는")
    class Describe_change_gwangsan {

        @Test
        @DisplayName("plus·minus·adjust 결과를 managed 상세 엔티티에 반영한다 (음수 잔액 허용, 조정만 0 하한)")
        void it_applies_balance_changes() {
            MemberDetailJpaEntity detail = detail(member(1L, "테스터일"), 100);
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.of(detail));
            ChangeGwangsanService service = new ChangeGwangsanService(persistence, persistence);

            service.plus(1L, 50);
            assertThat(detail.getGwangsan()).isEqualTo(150);
            service.minus(1L, 200);
            assertThat(detail.getGwangsan()).isEqualTo(-50);
            service.adjust(1L, -10);
            assertThat(detail.getGwangsan()).isZero();
            assertThat(detail.getLight()).isEqualTo(1);
            assertThat(detail.getDescription()).isEqualTo("기존 설명");
        }

        @Test
        @DisplayName("상세 정보가 없으면 NotFoundMemberDetailException을 던진다")
        void it_throws_when_detail_missing() {
            when(memberDetailJpaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> new ChangeGwangsanService(persistence, persistence).plus(1L, 1))
                    .isInstanceOf(NotFoundMemberDetailException.class);
        }
    }

    @Nested
    @DisplayName("RegisterMemberService.execute() 는")
    class Describe_register {

        @Test
        @DisplayName("승인 대기 일반 회원과 초기 상세(광산 0, 빛 1)를 저장하고 특기를 순서대로 연결한다")
        void it_registers_pending_member_with_initial_detail() {
            MemberJpaEntity recommender = member(9L, "추천인");
            DongJpaEntity dong = mock(DongJpaEntity.class);
            Place place = mock(Place.class);
            when(entityManager.getReference(MemberJpaEntity.class, 9L)).thenReturn(recommender);
            when(entityManager.getReference(DongJpaEntity.class, 3)).thenReturn(dong);
            when(entityManager.getReference(Place.class, 1)).thenReturn(place);
            MemberJpaEntity[] persisted = new MemberJpaEntity[1];
            when(memberJpaRepository.save(any(MemberJpaEntity.class))).thenAnswer(i -> {
                persisted[0] = i.getArgument(0);
                ReflectionTestUtils.setField(persisted[0], "id", 10L);
                return persisted[0];
            });
            RelatedKeywordJpaEntity java = RelatedKeywordJpaEntity.builder().name("Java").build();
            when(relatedKeywordRepository.findByName("Java")).thenReturn(Optional.of(java));
            when(relatedKeywordRepository.findByName("Spring")).thenReturn(Optional.empty());
            when(relatedKeywordRepository.save(any(RelatedKeywordJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
            when(entityManager.getReference(MemberJpaEntity.class, 10L)).thenAnswer(i -> persisted[0]);

            Long id = new RegisterMemberService(persistence, keywords).execute(new RegisterMemberCommand("홍길동",
                    "테스터일", "encoded", "01012345678", 9L, 3, 1, "자기소개", List.of("Java", "Spring")));

            assertThat(id).isEqualTo(10L);
            MemberJpaEntity member = persisted[0];
            assertThat(member.getRole()).isEqualTo(MemberRole.ROLE_USER);
            assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
            assertThat(member.getPassword()).isEqualTo("encoded");
            assertThat(member.getRecommender()).isSameAs(recommender);
            ArgumentCaptor<MemberDetailJpaEntity> savedDetail = ArgumentCaptor.forClass(MemberDetailJpaEntity.class);
            verify(memberDetailJpaRepository).save(savedDetail.capture());
            assertThat(savedDetail.getValue().getMember()).isSameAs(member);
            assertThat(savedDetail.getValue().getDong()).isSameAs(dong);
            assertThat(savedDetail.getValue().getPlace()).isSameAs(place);
            assertThat(savedDetail.getValue().getGwangsan()).isZero();
            assertThat(savedDetail.getValue().getLight()).isEqualTo(1);
            ArgumentCaptor<MemberRelatedKeywordJpaEntity> mappings = ArgumentCaptor.forClass(MemberRelatedKeywordJpaEntity.class);
            verify(memberRelatedKeywordRepository, times(2)).save(mappings.capture());
            assertThat(mappings.getAllValues()).extracting(m -> m.getRelatedKeyword().getName())
                    .containsExactly("Java", "Spring");
        }
    }

    @Nested
    @DisplayName("계정 조회·비밀번호 변경은")
    class Describe_account {

        @Test
        @DisplayName("회원 계정을 공개 data 로 변환하고 강제 탈퇴 이력을 조회한다")
        void it_maps_member_account() {
            when(memberJpaRepository.findByNickname("테스터일")).thenReturn(Optional.of(member(1L, "테스터일")));
            when(withdrawalRecordJpaRepository.existsByPhoneNumberAndBannedIsTrue("01099999999")).thenReturn(true);
            MemberAccountService service = new MemberAccountService(persistence);

            assertThat(service.findByNickname("테스터일")).contains(new MemberAccount(1L, "테스터일", "01012345678",
                    "encoded", MemberRole.ROLE_USER, MemberStatus.ACTIVE));
            assertThat(service.isBannedPhoneNumber("01099999999")).isTrue();
        }

        @Test
        @DisplayName("비밀번호 해시를 managed 엔티티에 반영한다")
        void it_changes_password() {
            MemberJpaEntity member = member(1L, "테스터일");
            when(memberJpaRepository.findById(1L)).thenReturn(Optional.of(member));

            new ChangePasswordService(persistence, persistence).execute(1L, "encodedNewPw");

            assertThat(member.getPassword()).isEqualTo("encodedNewPw");
            assertThat(member.getNickname()).isEqualTo("테스터일");
        }
    }
}
