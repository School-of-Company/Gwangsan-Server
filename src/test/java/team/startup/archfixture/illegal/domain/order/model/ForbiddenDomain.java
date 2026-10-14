package team.startup.archfixture.illegal.domain.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.repository.Repository;
import team.startup.archfixture.illegal.domain.member.model.Member;
import team.startup.archfixture.illegal.domain.order.entity.LegacyOrderEntity;

import java.util.List;

// annotation 의존
@Entity
class AnnotatedDomainOrder {
    @Id
    Long id;
}

// 상속 의존
interface InheritingDomainRepository extends Repository<Object, Long> {
}

// generic 타입 인자 의존
class GenericDomainOrder {
    List<HttpServletRequest> requests;
}

// 타 업무 domain 직접 참조
class CrossBusinessOrder {
    Member member;
}

// 전환 전 legacy 클래스 참조
class LegacyDependentOrder {
    LegacyOrderEntity legacy;
}
