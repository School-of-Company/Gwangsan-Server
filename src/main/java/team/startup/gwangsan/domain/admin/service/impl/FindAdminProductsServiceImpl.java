package team.startup.gwangsan.domain.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.admin.service.FindAdminProductsService;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.ProductStatus;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindAdminProductsServiceImpl implements FindAdminProductsService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MemberDetailRepository memberDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GetProductResponse> execute(Type type, Mode mode, Long lastId, int size) {
        List<Product> products = productRepository.findAdminProducts(type, mode, lastId, size);

        if (products.isEmpty()) {
            return List.of();
        }

        List<Long> memberIds = products.stream()
                .map(p -> p.getMember().getId())
                .distinct()
                .toList();

        Map<Long, MemberDetail> memberDetailMap = memberDetailRepository.findAllByMemberIdIn(memberIds).stream()
                .collect(Collectors.toMap(md -> md.getMember().getId(), md -> md));

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        Map<Long, List<GetImageResponse>> imageMap = productImageRepository
                .findAllByProductIdIn(productIds).stream()
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
                .map(product -> {
                    Member member = product.getMember();
                    MemberDetail memberDetail = memberDetailMap.get(member.getId());

                    int rawLight = memberDetail.getLight();
                    int light = Math.max(1, rawLight / 10);

                    GetProductMemberResponse memberResponse = new GetProductMemberResponse(
                            member.getId(),
                            member.getNickname(),
                            memberDetail.getPlace().getName(),
                            light
                    );

                    return new GetProductResponse(
                            product.getId(),
                            product.getTitle(),
                            product.getDescription(),
                            product.getGwangsan(),
                            product.getType(),
                            product.getMode(),
                            memberResponse,
                            imageMap.getOrDefault(product.getId(), List.of()),
                            product.getStatus() == ProductStatus.COMPLETED,
                            product.getStatus() == ProductStatus.RESERVATION
                    );
                })
                .toList();
    }
}
