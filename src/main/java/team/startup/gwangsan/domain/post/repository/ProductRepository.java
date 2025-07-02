package team.startup.gwangsan.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team.startup.gwangsan.domain.post.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
