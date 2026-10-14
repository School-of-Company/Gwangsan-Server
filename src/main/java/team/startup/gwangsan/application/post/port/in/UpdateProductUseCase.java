package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.UpdateProductCommand;

public interface UpdateProductUseCase {
    void execute(UpdateProductCommand command);
}
