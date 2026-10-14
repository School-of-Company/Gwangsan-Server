package team.startup.gwangsan.application.post.port.in;

import team.startup.gwangsan.application.post.data.CreateProductCommand;

public interface CreateProductUseCase {
    void execute(CreateProductCommand command);
}
