package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.ReserveProductCommand;

public interface ReserveProductUseCase {
    void execute(ReserveProductCommand command);
}
