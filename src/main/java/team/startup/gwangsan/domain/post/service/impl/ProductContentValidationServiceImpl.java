package team.startup.gwangsan.domain.post.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;
import team.startup.gwangsan.domain.post.service.ProductContentValidationService;
import team.startup.gwangsan.global.thirdparty.ai.AiModerationClient;

@Service
@RequiredArgsConstructor
public class ProductContentValidationServiceImpl implements ProductContentValidationService {

    private final AiModerationClient aiModerationClient;

    @Override
    public void validate(String title, String description) {
        if (aiModerationClient.containsProfanity(title + "\n" + description)) {
            throw new InappropriateContentException();
        }
    }
}
