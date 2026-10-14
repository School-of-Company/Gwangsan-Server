package team.startup.archfixture.illegal.application.order.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import team.startup.archfixture.illegal.adapter.persistence.order.OrderJpaEntity;
import team.startup.archfixture.illegal.application.member.service.MemberService;

// 메서드 호출 의존
class SecurityReadingService {
    Object actor() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

// persistence 모델 참조
class PersistenceUsingService {
    OrderJpaEntity entity;
}

// 타 업무 내부 service 참조
class CrossBusinessService {
    MemberService memberService;
}

@RestController
class ControllerInApplication {
}
