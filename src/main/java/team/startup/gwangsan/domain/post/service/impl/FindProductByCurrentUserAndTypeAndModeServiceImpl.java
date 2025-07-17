package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberDetailException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.FindProductByCurrentUserAndTypeAndModeService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindProductByCurrentUserAndTypeAndModeServiceImpl implements FindProductByCurrentUserAndTypeAndModeService {

    private final MemberDetailRepository memberDetailRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public List<GetProductResponse> execute(Type type, Mode mode) {
        Member member = memberUtil.getCurrentMember();
        MemberDetail memberDetail = memberDetailRepository.findById(member.getId())
                .orElseThrow(NotFoundMemberDetailException::new);
        List<Product> products = productRepository.findProductByMemberAndTypeAndModeAndStatus(member, type, mode, ProductStatus.ONGOING);

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<GetImageResponse>> imageMap = productImageRepository
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

        int rawLight = memberDetail.getLight();
        int light = Math.max(1, rawLight / 10);

        GetProductMemberResponse memberResponse = new GetProductMemberResponse(
                member.getId(),
                member.getNickname(),
                memberDetail.getPlace().getName(),
                light
        );

        return products.stream()
                .map(product -> new GetProductResponse(
                        product.getId(),
                        product.getTitle(),
                        product.getDescription(),
                        product.getGwangsan(),
                        product.getType(),
                        product.getMode(),
                        memberResponse,
                        imageMap.getOrDefault(product.getId(), List.of())
                ))
                .toList();
    }
}
