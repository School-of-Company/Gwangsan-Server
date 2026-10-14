package team.startup.gwangsan.adapter.web.post.dto;

import team.startup.gwangsan.application.post.data.ProductView;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public record GetProductResponse(
        Long id,
        String title,
        String content,
        Integer gwangsan,
        Type type,
        Mode mode,
        GetProductMemberResponse member,
        List<ProductImageResponse> images,
        boolean isCompleted,
        boolean isReserved
) {
    public static List<GetProductResponse> from(List<ProductView> views) {
        return views.stream().map(view -> new GetProductResponse(view.id(), view.title(), view.content(),
                view.gwangsan(), view.type(), view.mode(), GetProductMemberResponse.from(view.member()),
                ProductImageResponse.from(view.images()), view.completed(), view.reserved())).toList();
    }
}
