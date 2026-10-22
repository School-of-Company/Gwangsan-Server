package team.startup.gwangsan.application.feedback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.feedback.data.CreateFeedbackCommand;
import team.startup.gwangsan.application.feedback.port.in.CreateFeedbackUseCase;
import team.startup.gwangsan.application.feedback.port.out.LoadFeedbackImagePort;
import team.startup.gwangsan.application.feedback.port.out.SaveFeedbackPort;
import team.startup.gwangsan.domain.feedback.model.Feedback;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreateFeedbackService implements CreateFeedbackUseCase {

    private final SaveFeedbackPort saveFeedbackPort;
    private final LoadFeedbackImagePort loadFeedbackImagePort;

    @Override
    @Transactional
    public void execute(CreateFeedbackCommand command) {
        Feedback feedback = Feedback.create(command.actor().memberId(), command.type(), command.content(),
                command.appVersion(), command.osType(), command.osVersion(), command.deviceModel());
        Long feedbackId = saveFeedbackPort.save(feedback);

        List<Long> imageIds = command.imageIds();
        if (imageIds != null && !imageIds.isEmpty()) {
            List<Long> existingImageIds = loadFeedbackImagePort.findExistingImageIds(imageIds);
            saveFeedbackPort.saveImages(feedbackId, existingImageIds);
        }
    }
}
