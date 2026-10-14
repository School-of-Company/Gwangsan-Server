package team.startup.archfixture.legal.adapter.persistence.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrderJpaEntity {
    @Id
    Long id;
    Long ownerId;
    boolean confirmed;
}
