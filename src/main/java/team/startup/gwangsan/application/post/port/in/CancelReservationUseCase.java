package team.startup.gwangsan.application.post.port.in;

public interface CancelReservationUseCase {
    void execute(Long actorId, Long productId);
}
