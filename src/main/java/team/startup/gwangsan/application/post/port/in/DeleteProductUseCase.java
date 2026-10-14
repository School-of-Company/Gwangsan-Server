package team.startup.gwangsan.application.post.port.in;

public interface DeleteProductUseCase {
    void execute(Long actorId, Long productId);
}
