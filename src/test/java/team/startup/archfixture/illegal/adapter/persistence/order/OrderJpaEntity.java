package team.startup.archfixture.illegal.adapter.persistence.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrderJpaEntity {
    @Id
    Long id;
}
