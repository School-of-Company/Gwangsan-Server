package team.startup.gwangsan.application.post.data;

import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

public record ProductDetailView(Long id, String title, String content, Integer gwangsan, Type type, Mode mode,
                                ProductAuthor member, List<ImageView> images, boolean mine, boolean completable,
                                boolean completed, boolean reserved) {
}
