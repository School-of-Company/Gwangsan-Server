package team.startup.gwangsan.domain.post.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.startup.gwangsan.domain.post.exception.InappropriateContentException;
import team.startup.gwangsan.global.thirdparty.ai.AiModerationClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductContentValidationServiceImpl 단위 테스트")
class ProductContentValidationServiceImplTest {

    @InjectMocks
    private ProductContentValidationServiceImpl service;

    @Mock
    private AiModerationClient aiModerationClient;

    @Nested
    @DisplayName("validate() 메서드는")
    class Describe_validate {

        @Test
        @DisplayName("부적절한 내용이 있으면 InappropriateContentException을 던진다")
        void it_throws_exception_for_inappropriate_content() {
            when(aiModerationClient.containsProfanity("제목\n설명")).thenReturn(true);

            assertThatThrownBy(() -> service.validate("제목", "설명"))
                    .isInstanceOf(InappropriateContentException.class);
        }
    }
}
