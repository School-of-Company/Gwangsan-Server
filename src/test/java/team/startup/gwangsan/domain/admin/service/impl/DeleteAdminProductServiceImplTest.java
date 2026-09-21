package team.startup.gwangsan.domain.admin.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.constant.MemberRole;
import team.startup.gwangsan.domain.member.entity.constant.MemberStatus;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAdminProductServiceImpl 단위 테스트")
class DeleteAdminProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private DeleteAdminProductServiceImpl service;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Test
        @DisplayName("활성 상품의 실제 상태를 DELETED로 변경한다")
        void it_changes_real_product_status_to_deleted() {
            Product product = Product.builder().title("상품").description("설명").gwangsan(100)
                    .member(Member.builder().name("회원").nickname("회원").phoneNumber("010").password("password")
                            .role(MemberRole.ROLE_USER).status(MemberStatus.ACTIVE).build())
                    .type(Type.SERVICE).mode(Mode.GIVER).status(ProductStatus.ONGOING).build();
            ReflectionTestUtils.setField(product, "id", 1L);
            when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

            service.execute(1L);

            assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
            verify(productRepository).findActiveById(1L);
        }

        @Test
        @DisplayName("활성 상품이 없으면 NotFoundProductException을 던진다")
        void it_throws_not_found_product_exception() {
            when(productRepository.findActiveById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(1L)).isInstanceOf(NotFoundProductException.class);
        }
    }
}
