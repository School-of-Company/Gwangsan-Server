package team.startup.gwangsan.application.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.application.post.data.ProductSnapshot;
import team.startup.gwangsan.application.post.port.out.ProductImagePort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ReservationParticipantOnlyException;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductReservation;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.ReservationStatus;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("다른 업무에 공개하는 게시글 계약")
class PostPublicContractServiceTest {

    @Mock ProductPort productPort;
    @Mock ProductImagePort productImagePort;
    @Mock ReservationPort reservationPort;

    private static Product product(ProductStatus status) {
        return new Product(7L, 10L, Type.OBJECT, Mode.GIVER, "제목", "설명", 5000, status);
    }

    private static ProductReservation reservation(Long reserverId) {
        return new ProductReservation(3L, 7L, reserverId, ReservationStatus.PENDING, null, null, null, null, null,
                null, null);
    }

    @Nested
    @DisplayName("조회 Query 는")
    class Describe_query {

        private ProductPublicQueryService service() {
            return new ProductPublicQueryService(productPort, productImagePort);
        }

        @Test
        @DisplayName("저장 모델 대신 snapshot 을 돌려주고 없으면 NotFoundProductException")
        void it_returns_snapshot_or_throws() {
            when(productPort.findActive(7L)).thenReturn(Optional.of(product(ProductStatus.COMPLETED)));
            when(productPort.find(8L)).thenReturn(Optional.empty());

            ProductSnapshot snapshot = service().getActive(7L);

            assertThat(snapshot).isEqualTo(new ProductSnapshot(7L, 10L, "제목", "설명", 5000, Type.OBJECT, Mode.GIVER,
                    ProductStatus.COMPLETED));
            assertThat(snapshot.isCompleted()).isTrue();
            assertThat(snapshot.isGiver()).isTrue();
            assertThatThrownBy(() -> service().get(8L)).isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("묶음 조회는 빈 입력이면 저장소를 부르지 않는다")
        void it_skips_empty_batch() {
            assertThat(service().findAll(Set.of())).isEmpty();
            assertThat(service().findImages(List.of())).isEmpty();
            verifyNoInteractions(productPort, productImagePort);
        }

        @Test
        @DisplayName("묶음 조회는 id 로 색인한다")
        void it_indexes_by_id() {
            when(productPort.findAll(Set.of(7L))).thenReturn(List.of(product(ProductStatus.DELETED)));

            Map<Long, ProductSnapshot> result = service().findAll(Set.of(7L));

            assertThat(result.get(7L).isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("상태 변경 command 는")
    class Describe_status {

        private ProductStatusService service() {
            return new ProductStatusService(productPort);
        }

        @Test
        @DisplayName("잠금 결과를 snapshot 으로 돌려주고 없으면 NotFoundProductException")
        void it_locks() {
            when(productPort.lockActive(7L)).thenReturn(Optional.of(product(ProductStatus.ONGOING)));
            when(productPort.lockIncludingDeleted(8L)).thenReturn(Optional.empty());

            assertThat(service().lockActive(7L).status()).isEqualTo(ProductStatus.ONGOING);
            assertThatThrownBy(() -> service().lockIncludingDeleted(8L)).isInstanceOf(NotFoundProductException.class);
        }

        @Test
        @DisplayName("complete·reopen 은 삭제 포함 조회 후 상태를 저장한다")
        void it_completes_and_reopens() {
            when(productPort.find(7L)).thenReturn(Optional.of(product(ProductStatus.DELETED)));
            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);

            service().complete(7L);
            verify(productPort).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.COMPLETED);

            service().reopen(7L);
            assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.ONGOING);
        }

        @Test
        @DisplayName("관리자 삭제는 예약 중이어도 DELETED 로 바꾸고, 삭제된 글이면 NotFoundProductException")
        void it_deletes_by_admin() {
            when(productPort.findActive(7L)).thenReturn(Optional.of(product(ProductStatus.RESERVATION)));
            when(productPort.findActive(8L)).thenReturn(Optional.empty());
            ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);

            service().deleteByAdmin(7L);

            verify(productPort).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(ProductStatus.DELETED);
            assertThatThrownBy(() -> service().deleteByAdmin(8L)).isInstanceOf(NotFoundProductException.class);
        }
    }

    @Nested
    @DisplayName("진행 중 예약 계약은")
    class Describe_pending_reservation {

        private PendingReservationService service() {
            return new PendingReservationService(reservationPort);
        }

        @Test
        @DisplayName("예약자가 구매자나 판매자면 예약 ID 를 돌려준다")
        void it_returns_reservation_id_for_participant() {
            when(reservationPort.findPending(7L)).thenReturn(Optional.of(reservation(21L)));

            assertThat(service().requireParticipant(7L, 20L, 21L)).isEqualTo(3L);
        }

        @Test
        @DisplayName("예약이 없거나 당사자가 아니면 ReservationParticipantOnlyException")
        void it_rejects_non_participant() {
            when(reservationPort.findPending(7L)).thenReturn(Optional.of(reservation(99L)));
            when(reservationPort.findPending(8L)).thenReturn(Optional.empty());
            when(reservationPort.find(4L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().requireParticipant(7L, 20L, 21L))
                    .isInstanceOf(ReservationParticipantOnlyException.class);
            assertThatThrownBy(() -> service().requireParticipant(8L, 20L, 21L))
                    .isInstanceOf(ReservationParticipantOnlyException.class);
            assertThatThrownBy(() -> service().complete(4L))
                    .isInstanceOf(ReservationParticipantOnlyException.class);
            verify(reservationPort, never()).save(any());
        }

        @Test
        @DisplayName("complete 는 COMPLETED 로 저장한다")
        void it_completes() {
            when(reservationPort.find(3L)).thenReturn(Optional.of(reservation(21L)));
            ArgumentCaptor<ProductReservation> saved = ArgumentCaptor.forClass(ProductReservation.class);

            service().complete(3L);

            verify(reservationPort).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("참조 정리 command 는")
    class Describe_cleanup {

        @Test
        @DisplayName("작성자·예약자 재지정과 이미지 연결 삭제를 저장 Port 에 위임한다")
        void it_delegates() {
            PostReferenceCleanupService service =
                    new PostReferenceCleanupService(productPort, reservationPort, productImagePort);

            service.reassignAuthor(5L, 1L);
            service.reassignReserver(5L, 1L);
            service.unlink(7L, 11L);

            verify(productPort).reassignAuthor(5L, 1L);
            verify(reservationPort).reassignReserver(5L, 1L);
            verify(productImagePort).unlink(7L, 11L);
        }
    }
}
