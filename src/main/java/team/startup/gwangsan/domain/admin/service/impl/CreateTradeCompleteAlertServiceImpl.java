package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.CreateTradeCompleteAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.repository.MemberRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.global.event.CreateAlertEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreateTradeCompleteAlertServiceImpl implements CreateTradeCompleteAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String TRADE_COMPLETE_TITLE = "거래 완료 요청";

    @Override
    @Transactional
    public void execute(AlertType type, Long sourceId, Long otherMemberId, Long requesterMemberId) {
        Product product = productRepository.findById(sourceId)
                .orElseThrow(NotFoundProductException::new);

        Map<Long, Member> memberMap = memberRepository.findAllByIdIn(List.of(otherMemberId, requesterMemberId))
                .stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        Member otherMember = memberMap.get(otherMemberId);
        Member requesterMember = memberMap.get(requesterMemberId);

        AdminAlert alert = AdminAlert.builder()
                .type(type)
                .title(TRADE_COMPLETE_TITLE)
                .sourceId(sourceId)
                .otherMember(otherMember)
                .requester(requesterMember)
                .build();
        adminAlertRepository.save(alert);
    }
}
