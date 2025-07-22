package team.startup.gwangsan.domain.alert.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.alert.entity.Alert;
import team.startup.gwangsan.domain.alert.entity.constant.AlertType;
import team.startup.gwangsan.domain.alert.presentation.dto.response.GetAlertResponse;
import team.startup.gwangsan.domain.alert.repository.AlertRepository;
import team.startup.gwangsan.domain.alert.service.FindAlertByCurrentService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.notice.repository.NoticeImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAlertByCurrentServiceImpl implements FindAlertByCurrentService {

    private final MemberUtil memberUtil;
    private final AlertRepository alertRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetAlertResponse> execute() {
        Member member = memberUtil.getCurrentMember();
        List<Alert> alerts = alertRepository.findAllByMember(member);

        List<Long> noticeIds = alerts.stream()
                .filter(a -> a.getAlertType() == AlertType.NOTICE)
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> productIds = alerts.stream()
                .filter(a -> a.getAlertType() == AlertType.TRADE_COMPLETE
                        || a.getAlertType() == AlertType.TRADE_COMPLETE_REJECT
                        || a.getAlertType() == AlertType.OTHER_MEMBER_TRADE_COMPLETE)
                .map(Alert::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<GetImageResponse>> noticeImageMap = noticeImageRepository.findAllByNoticeIdIn(noticeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ni -> ni.getNotice().getId(),
                        Collectors.mapping(
                                ni -> new GetImageResponse(ni.getId(), ni.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        Map<Long, List<GetImageResponse>> productImageMap = productImageRepository.findProductImageByProductIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(
                                pi -> new GetImageResponse(pi.getId(), pi.getImage().getImageUrl()),
                                Collectors.toList()
                        )
                ));

        return alerts.stream()
                .map(alert -> {
                    List<GetImageResponse> images = switch (alert.getAlertType()) {
                        case NOTICE -> noticeImageMap.getOrDefault(alert.getSourceId(), List.of());
                        case TRADE_COMPLETE, TRADE_COMPLETE_REJECT, OTHER_MEMBER_TRADE_COMPLETE ->
                                productImageMap.getOrDefault(alert.getSourceId(), List.of());
                        default -> List.of();
                    };

                    return new GetAlertResponse(
                            alert.getId(),
                            alert.getTitle(),
                            alert.getContent(),
                            alert.getAlertType(),
                            alert.getCreatedAt(),
                            images,
                            alert.getSendMember() != null ? alert.getSendMember().getId() : null,
                            alert.getSourceId()
                    );
                })
                .toList();
    }
}
