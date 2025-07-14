package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.image.presentation.dto.response.GetImageResponse;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.member.entity.MemberDetail;
import team.startup.gwangsan.domain.member.exception.NotFoundMemberException;
import team.startup.gwangsan.domain.member.repository.MemberDetailRepository;
import team.startup.gwangsan.domain.place.entity.Place;
import team.startup.gwangsan.domain.place.exception.PlaceMismatchException;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductMemberResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.repository.ProductImageRepository;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.FindProductByIdService;
import team.startup.gwangsan.global.util.MemberUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindProductByIdServiceImpl implements FindProductByIdService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberDetailRepository memberDetailRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional(readOnly = true)
    public GetProductResponse execute(Long id) {
        Member currentMember = memberUtil.getCurrentMember();
        return execute(currentMember, id);
    }

    @Override
    @Transactional(readOnly = true)
    public GetProductResponse execute(Member member, Long id) {
        Place myPlace = memberDetailRepository.findPlaceByMemberId(member.getId());

        Product product = productRepository.findById(id)
                .orElseThrow(NotFoundProductException::new);
        Place productPlace = memberDetailRepository.findPlaceByMemberId(product.getMember().getId());

        MemberDetail memberDetail = memberDetailRepository.findById(product.getMember().getId())
                .orElseThrow(NotFoundMemberException::new);

        validateSamePlace(myPlace, productPlace);

        List<GetImageResponse> images = productImageRepository.findByProductId(product.getId())
                .stream()
                .map(pi -> new GetImageResponse(
                        pi.getImage().getId(),
                        pi.getImage().getImageUrl()
                ))
                .toList();

        int rawLight = memberDetail.getLight();
        int light = Math.max(1, rawLight / 10);

        GetProductMemberResponse memberResponse = new GetProductMemberResponse(
                memberDetail.getId(),
                memberDetail.getMember().getNickname(),
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
                images
        );
    }

    private void validateSamePlace(Place myPlace, Place productPlace) {
        if (!productPlace.isSamePlace(myPlace)) {
            throw new PlaceMismatchException();
        }
    }
}
