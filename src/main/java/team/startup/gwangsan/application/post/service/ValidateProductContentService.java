package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.application.post.port.in.ValidateProductContentUseCase;
import team.startup.gwangsan.application.post.port.out.ContentModerationPort;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;

@Service
@RequiredArgsConstructor
public class ValidateProductContentService implements ValidateProductContentUseCase {

    private final ContentModerationPort contentModerationPort;

    @Override
    public void execute(String title, String description) {
        if (contentModerationPort.containsProfanity(title + "\n" + description)) {
            throw new InappropriateContentException();
        }
    }
}
