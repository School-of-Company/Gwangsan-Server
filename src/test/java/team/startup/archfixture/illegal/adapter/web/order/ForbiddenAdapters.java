package team.startup.archfixture.illegal.adapter.web.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.web.bind.annotation.RestController;
import team.startup.archfixture.illegal.adapter.persistence.order.OrderJpaRepository;
import team.startup.archfixture.illegal.application.member.service.MemberService;

// input Port 대신 application service 직접 호출
@RestController
class ServiceCallingController {
    MemberService memberService;
}

// inbound adapter 의 Repository 직접 사용
class RepositoryCallingController {
    OrderJpaRepository repository;
}

// persistence 밖 JPA Entity
@Entity
class EntityInWeb {
    @Id
    Long id;
}
