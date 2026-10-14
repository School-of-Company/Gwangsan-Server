package team.startup.archfixture.illegal.domain.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// fixture legacy 목록에 등록된 전환 전 클래스. 자신은 검사 대상이 아니다.
@Entity
public class LegacyOrderEntity {
    @Id
    Long id;
}
