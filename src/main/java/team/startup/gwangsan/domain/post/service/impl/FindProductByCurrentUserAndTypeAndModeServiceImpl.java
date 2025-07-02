package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.custom.ProductCustomRepository;
import team.startup.gwangsan.domain.post.repository.custom.ProductImageCustomRepository;
import team.startup.gwangsan.domain.post.service.FindProductByCurrentUserAndTypeAndModeService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindProductByCurrentUserAndTypeAndModeServiceImpl implements FindProductByCurrentUserAndTypeAndModeService {

    private final ProductImageCustomRepository productImageCustomRepository;
    private final ProductCustomRepository productCustomRepository;
    private final MemberUtil memberUtil;

    @Override
    public List<GetProductResponse> execute(Type type, Mode mode) {
        Member member = memberUtil.getCurrentMember();
        List<Product> products = productCustomRepository.findProductByMemberAndTypeAndMode(member, type, mode);

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
