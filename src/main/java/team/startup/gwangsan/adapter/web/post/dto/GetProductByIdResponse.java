package team.startup.gwangsan.adapter.web.post.dto;

import team.startup.gwangsan.application.post.data.ProductDetailView;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public record GetProductByIdResponse(
        Long id,
        String title,
        String content,
        Integer gwangsan,
        Type type,
        Mode mode,
        GetProductMemberResponse member,
        List<ProductImageResponse> images,
        boolean isMine,
        boolean isCompletable,
        boolean isCompleted,
        boolean isReserved
) {
    public static GetProductByIdResponse from(ProductDetailView view) {
        return new GetProductByIdResponse(view.id(), view.title(), view.content(), view.gwangsan(), view.type(),
                view.mode(), GetProductMemberResponse.from(view.member()), ProductImageResponse.from(view.images()),
                view.mine(), view.completable(), view.completed(), view.reserved());
    }
}
