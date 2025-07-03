package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.repository.custom.MemberDetailCustomRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.custom.ProductCustomRepository;
import team.startup.gwangsan.domain.post.repository.custom.ProductImageCustomRepository;
import team.startup.gwangsan.domain.post.service.FindProductsByTypeAndModeService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindProductsByTypeAndModeServiceImpl implements FindProductsByTypeAndModeService {

    private final ProductImageCustomRepository productImageCustomRepository;
    private final ProductCustomRepository productCustomRepository;
    private final MemberDetailCustomRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<GetProductResponse> execute(Type type, Mode mode) {
        Place myPlace = memberDetailRepository.findPlaceByMemberId(memberUtil.getCurrentMember().getId());

        List<Product> products = productCustomRepository.findProductsByTypeAndModeAndMemberDetailPlace(type, mode, myPlace);

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<GetImageResponse>> imageMap = productImageCustomRepository
                .findProductImageByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        pi -> pi.getProduct().getId(),
                        Collectors.mapping(
                                pi -> new GetImageResponse(
                                        pi.getImage().getId(),
                                        pi.getImage().getImageUrl()
                                ),
                                Collectors.toList()
                        )
                ));

        return products.stream()
                .map(product -> new GetProductResponse(
                        product.getId(),
                        product.getTitle(),
                        product.getDescription(),
                        product.getGwangsan(),
                        product.getType(),
                        product.getMode(),
                        imageMap.getOrDefault(product.getId(), List.of())
                ))
                .toList();
    }
}
