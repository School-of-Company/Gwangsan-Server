package team.startup.gwangsan.application.review.port.in;

import team.startup.gwangsan.application.review.data.CreateReviewCommand;

public interface CreateReviewUseCase {
    void execute(CreateReviewCommand command);
}
