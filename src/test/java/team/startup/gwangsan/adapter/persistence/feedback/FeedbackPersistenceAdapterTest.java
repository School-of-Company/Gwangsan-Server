package team.startup.gwangsan.adapter.persistence.feedback;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.feedback.model.Feedback;
import team.startup.gwangsan.domain.feedback.model.FeedbackStatus;
import team.startup.gwangsan.domain.feedback.model.FeedbackType;
import team.startup.gwangsan.domain.notification.model.OsType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackPersistenceAdapter 단위 테스트")
class FeedbackPersistenceAdapterTest {

    @Mock private FeedbackJpaRepository feedbackJpaRepository;
    @Mock private FeedbackImageJpaRepository feedbackImageJpaRepository;
    @Mock private EntityManager entityManager;

    private FeedbackPersistenceAdapter adapter() {
        return new FeedbackPersistenceAdapter(feedbackJpaRepository, feedbackImageJpaRepository, entityManager);
    }

    private MemberJpaEntity member(Long id) {
        MemberJpaEntity member = mock(MemberJpaEntity.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    @Nested
    @DisplayName("save() 는")
    class Describe_save {

        @Test
        @DisplayName("작성자 참조를 얻어 문의를 저장하고 생성된 ID를 돌려준다")
        void it_saves_and_returns_id() {
            MemberJpaEntity member = member(1L);
            when(entityManager.getReference(MemberJpaEntity.class, 1L)).thenReturn(member);
            when(feedbackJpaRepository.save(any())).thenAnswer(i -> {
                FeedbackJpaEntity entity = i.getArgument(0);
                ReflectionTestUtils.setField(entity, "id", 10L);
                return entity;
            });

            Long id = adapter().save(Feedback.create(1L, FeedbackType.BUG, "내용", "1.0", OsType.IOS, "18.0", "모델"));

            assertThat(id).isEqualTo(10L);
            ArgumentCaptor<FeedbackJpaEntity> captor = ArgumentCaptor.forClass(FeedbackJpaEntity.class);
            verify(feedbackJpaRepository).save(captor.capture());
            assertThat(captor.getValue().getMember()).isSameAs(member);
            assertThat(captor.getValue().getType()).isEqualTo(FeedbackType.BUG);
            assertThat(captor.getValue().getStatus()).isEqualTo(FeedbackStatus.RECEIVED);
        }
    }

    @Nested
    @DisplayName("saveImages() 는")
    class Describe_save_images {

        @Test
        @DisplayName("각 이미지 ID마다 연결 행을 만들어 저장한다")
        void it_saves_image_links() {
            FeedbackJpaEntity feedback = mock(FeedbackJpaEntity.class);
            ImageJpaEntity image1 = mock(ImageJpaEntity.class);
            ImageJpaEntity image2 = mock(ImageJpaEntity.class);
            when(entityManager.getReference(FeedbackJpaEntity.class, 10L)).thenReturn(feedback);
            when(entityManager.getReference(ImageJpaEntity.class, 1L)).thenReturn(image1);
            when(entityManager.getReference(ImageJpaEntity.class, 2L)).thenReturn(image2);

            adapter().saveImages(10L, List.of(1L, 2L));

            ArgumentCaptor<List<FeedbackImageJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(feedbackImageJpaRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
            assertThat(captor.getValue()).extracting(FeedbackImageJpaEntity::getImage).containsExactly(image1, image2);
        }
    }

    @Nested
    @DisplayName("deleteImageLink() 는")
    class Describe_delete_image_link {

        @Test
        @DisplayName("연결 행 삭제를 그대로 위임한다")
        void it_delegates() {
            adapter().deleteImageLink(10L, 1L);

            verify(feedbackImageJpaRepository).deleteByFeedbackIdAndImageId(10L, 1L);
        }
    }

    @Nested
    @DisplayName("saveAnswer() 는")
    class Describe_save_answer {

        @Test
        @DisplayName("managed 엔티티에 답변·답변자·상태를 반영한다")
        void it_applies_answer() {
            FeedbackJpaEntity entity = FeedbackJpaEntity.builder()
                    .member(member(1L)).type(FeedbackType.ETC).content("내용").status(FeedbackStatus.RECEIVED).build();
            when(feedbackJpaRepository.findById(10L)).thenReturn(Optional.of(entity));
            MemberJpaEntity admin = member(9L);
            when(entityManager.getReference(MemberJpaEntity.class, 9L)).thenReturn(admin);
            LocalDateTime now = LocalDateTime.now();

            Feedback feedback = Feedback.of(10L, 1L, FeedbackType.ETC, "내용", null, null, null, null,
                    FeedbackStatus.DONE, "답변", 9L, now, null);

            adapter().saveAnswer(feedback);

            assertThat(entity.getAnswer()).isEqualTo("답변");
            assertThat(entity.getAnsweredBy()).isSameAs(admin);
            assertThat(entity.getAnsweredAt()).isEqualTo(now);
            assertThat(entity.getStatus()).isEqualTo(FeedbackStatus.DONE);
        }
    }

    @Nested
    @DisplayName("changeStatus() 는")
    class Describe_change_status {

        @Test
        @DisplayName("managed 엔티티의 상태만 바꾼다")
        void it_changes_status() {
            FeedbackJpaEntity entity = FeedbackJpaEntity.builder()
                    .member(member(1L)).type(FeedbackType.ETC).content("내용").status(FeedbackStatus.RECEIVED).build();
            when(feedbackJpaRepository.findById(10L)).thenReturn(Optional.of(entity));

            adapter().changeStatus(Feedback.of(10L, 1L, FeedbackType.ETC, "내용", null, null, null, null,
                    FeedbackStatus.IN_PROGRESS, null, null, null, null));

            assertThat(entity.getStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("reassignMember() 는")
    class Describe_reassign_member {

        @Test
        @DisplayName("작성자·답변자 참조를 모두 재지정한다")
        void it_reassigns_both_references() {
            MemberJpaEntity target = member(1L);
            MemberJpaEntity dummy = member(99L);
            when(entityManager.getReference(MemberJpaEntity.class, 1L)).thenReturn(target);
            when(entityManager.getReference(MemberJpaEntity.class, 99L)).thenReturn(dummy);

            adapter().reassignMember(1L, 99L);

            verify(feedbackJpaRepository).reassignMember(target, dummy);
            verify(feedbackJpaRepository).reassignAnsweredBy(target, dummy);
        }
    }

    @Nested
    @DisplayName("findById() 는")
    class Describe_find_by_id {

        @Test
        @DisplayName("JPA 엔티티를 도메인 모델로 변환한다")
        void it_maps_to_domain() {
            FeedbackJpaEntity entity = FeedbackJpaEntity.builder()
                    .member(member(1L)).type(FeedbackType.BUG).content("내용").appVersion("1.0").osType(OsType.IOS)
                    .osVersion("18.0").deviceModel("모델").status(FeedbackStatus.RECEIVED).build();
            ReflectionTestUtils.setField(entity, "id", 10L);
            when(feedbackJpaRepository.findById(10L)).thenReturn(Optional.of(entity));

            Feedback feedback = adapter().findById(10L).orElseThrow();

            assertThat(feedback.getId()).isEqualTo(10L);
            assertThat(feedback.getMemberId()).isEqualTo(1L);
            assertThat(feedback.getType()).isEqualTo(FeedbackType.BUG);
            assertThat(feedback.getAnsweredByMemberId()).isNull();
        }
    }
}
