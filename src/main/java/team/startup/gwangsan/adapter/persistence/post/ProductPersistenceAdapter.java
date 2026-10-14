package team.startup.gwangsan.adapter.persistence.post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.adapter.persistence.member.MemberJpaEntity;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.ProductStatus;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.domain.post.model.Product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 게시글 저장. 같은 패키지의 JPA Entity {@code Product}(저장 모델)와 domain {@link Product} 를 명시적으로 변환한다.
 * 이름이 겹쳐 저장 모델은 FQN 으로 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductPort {

    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    @Override
    public Optional<Product> findActive(Long productId) {
        return productRepository.findActiveById(productId).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Product> lockActive(Long productId) {
        return productRepository.findByIdWithLock(productId).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Product> find(Long productId) {
        return productRepository.findById(productId).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Product> lockIncludingDeleted(Long productId) {
        return productRepository.findByIdForUpdate(productId).map(ProductPersistenceAdapter::toDomain);
    }

    @Override
    public List<Product> findAll(Collection<Long> productIds) {
        return productRepository.findAllById(productIds).stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findActivePage(Type type, Mode mode, Long lastId, int size) {
        return productRepository.findAdminProducts(type, mode, lastId, size).stream()
                .map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void reassignAuthor(Long memberId, Long dummyMemberId) {
        productRepository.reassignMember(entityManager.getReference(MemberJpaEntity.class, memberId),
                entityManager.getReference(MemberJpaEntity.class, dummyMemberId));
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            team.startup.gwangsan.adapter.persistence.post.Product entity = team.startup.gwangsan.adapter.persistence.post.Product.builder()
                    .title(product.getTitle())
                    .description(product.getDescription())
                    .gwangsan(product.getGwangsan())
                    .member(entityManager.getReference(MemberJpaEntity.class, product.getAuthorId()))
                    .type(product.getType())
                    .mode(product.getMode())
                    .status(product.getStatus())
                    .build();
            return toDomain(productRepository.save(entity));
        }
        // 같은 트랜잭션에서 조회한 managed Entity 에 명시적으로 반영한다.
        team.startup.gwangsan.adapter.persistence.post.Product entity = productRepository.findById(product.getId())
                .orElseThrow(IllegalStateException::new);
        entity.update(product.getType(), product.getMode(), product.getTitle(), product.getDescription(),
                product.getGwangsan());
        entity.updateStatus(product.getStatus());
        return product;
    }

    @Override
    public List<Product> findOngoingInPlace(Type type, Mode mode, Integer placeId) {
        return productRepository.findProductsByTypeAndModeAndMemberDetailPlaceAndStatus(
                        type, mode, entityManager.getReference(Place.class, placeId), ProductStatus.ONGOING)
                .stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findByAuthor(Long authorId, Type type, Mode mode, Collection<ProductStatus> statuses) {
        return productRepository.findProductByMemberAndTypeAndModeAndStatusIn(
                        entityManager.getReference(MemberJpaEntity.class, authorId), type, mode, statuses)
                .stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findAuthorPage(Long authorId, Type type, Mode mode, Long lastId, int size, Boolean completed) {
        return productRepository.findMemberProducts(authorId, type, mode, lastId, size, completed)
                .stream().map(ProductPersistenceAdapter::toDomain).toList();
    }

    static Product toDomain(team.startup.gwangsan.adapter.persistence.post.Product entity) {
        return new Product(entity.getId(), entity.getMember().getId(), entity.getType(), entity.getMode(),
                entity.getTitle(), entity.getDescription(), entity.getGwangsan(), entity.getStatus());
    }
}
