package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.entity.AdminAlert;
import team.startup.gwangsan.domain.admin.entity.constant.AlertType;
import team.startup.gwangsan.domain.admin.repository.AdminAlertRepository;
import team.startup.gwangsan.domain.admin.service.CreateTradeCompleteAlertService;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

@Service
@RequiredArgsConstructor
public class CreateTradeCompleteAlertServiceImpl implements CreateTradeCompleteAlertService {

    private final AdminAlertRepository adminAlertRepository;
    private final ProductRepository productRepository;

    private static final String TRADE_COMPLETE_TITLE = "거래 완료 요청";

    @Override
    @Transactional
    public void execute(AlertType type, Long sourceId, Member otherMember, Member requesterMember) {
        Product product = productRepository.findById(sourceId)
                .orElseThrow(NotFoundProductException::new);

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
