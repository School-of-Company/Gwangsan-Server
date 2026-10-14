package team.startup.gwangsan.adapter.web.post.dto;

import team.startup.gwangsan.application.post.data.ImageView;

import java.util.List;

public record ProductImageResponse(Long imageId, String imageUrl) {

    static List<ProductImageResponse> from(List<ImageView> images) {
        return images.stream().map(image -> new ProductImageResponse(image.imageId(), image.imageUrl())).toList();
    }
}
