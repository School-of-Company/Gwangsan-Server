package team.startup.gwangsan.adapter.persistence.post;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.startup.gwangsan.adapter.persistence.image.ImageJpaEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tbl_product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageJpaEntity image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    public ProductImage(ImageJpaEntity image, Product product) {
        this.image = image;
        this.product = product;
    }
}
