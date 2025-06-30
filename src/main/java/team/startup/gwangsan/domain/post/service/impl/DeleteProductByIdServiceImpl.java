package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.domain.member.entity.Member;
import team.startup.gwangsan.domain.post.entity.Product;
import team.startup.gwangsan.domain.post.exception.ForbiddenProductException;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.repository.ProductRepository;
import team.startup.gwangsan.domain.post.service.DeleteProductByIdService;
import team.startup.gwangsan.global.util.MemberUtil;

@Service
@RequiredArgsConstructor
public class DeleteProductByIdServiceImpl implements DeleteProductByIdService {

    private final ProductRepository productRepository;
    private final MemberUtil memberUtil;

    @Override
    @Transactional
    public void execute(Long id) {
        Member member = memberUtil.getCurrentMember();

        Product product = productRepository.findById(id)
                .orElseThrow(NotFoundProductException::new);

        validateProductMember(member, product.getMember());

        productRepository.delete(product);
    }

    private void validateProductMember(Member member, Member productMember) {
        if (!productMember.getId().equals(member.getId())) {
            throw new ForbiddenProductException();
        }
    }
}
