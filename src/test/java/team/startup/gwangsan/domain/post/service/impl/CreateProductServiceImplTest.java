package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.image.entity.Image;
import team.startup.gwangsan.domain.image.repository.ImageRepository;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.ProductImage;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProductServiceImpl 단위 테스트")
class CreateProductServiceImplTest {

    @InjectMocks
    private CreateProductServiceImpl service;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private MemberUtil memberUtil;

    @Nested
    @DisplayName("execute() 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 입력이 주어졌을 때")
        class Context_with_valid_request {

            @Test
            @DisplayName("상품과 ProductImage 리스트를 저장한다")
            void it_saves_product_and_images() {
                // given
                Type type = Type.OBJECT;
                Mode mode = Mode.GIVER;
                String title = "상품 제목";
                String description = "설명";
                int gwangsan = 100;

                List<Long> imageIds = List.of(1L, 2L);

                Member member = mock(Member.class);
                Image image1 = mock(Image.class);
                Image image2 = mock(Image.class);

                when(image1.getId()).thenReturn(1L);
                when(image2.getId()).thenReturn(2L);

                when(imageRepository.findByIdIn(imageIds))
                        .thenReturn(List.of(image1, image2));

                when(memberUtil.getCurrentMember()).thenReturn(member);

                ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
                ArgumentCaptor<List<ProductImage>> imageCaptor = ArgumentCaptor.forClass(List.class);

                // when
                service.execute(type, mode, title, description, gwangsan, imageIds);

                // then
                verify(productRepository).save(productCaptor.capture());
                Product saved = productCaptor.getValue();

                assertThat(saved.getTitle()).isEqualTo(title);
                assertThat(saved.getDescription()).isEqualTo(description);
                assertThat(saved.getGwangsan()).isEqualTo(gwangsan);
                assertThat(saved.getType()).isEqualTo(type);
                assertThat(saved.getMode()).isEqualTo(mode);
                assertThat(saved.getMember()).isEqualTo(member);
                assertThat(saved.getStatus()).isEqualTo(ProductStatus.ONGOING);

                verify(productImageRepository).saveAll(imageCaptor.capture());
                List<ProductImage> savedImages = imageCaptor.getValue();

                assertThat(savedImages).hasSize(2);
                assertThat(savedImages.get(0).getProduct()).isEqualTo(saved);
                assertThat(savedImages.get(1).getProduct()).isEqualTo(saved);
            }
        }
    }
}