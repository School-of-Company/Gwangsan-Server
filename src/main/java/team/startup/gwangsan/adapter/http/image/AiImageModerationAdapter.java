package team.startup.gwangsan.adapter.http.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import team.startup.gwangsan.application.image.port.out.ImageModerationPort;
import team.startup.gwangsan.global.thirdparty.ai.AiModerationClient;

// bridge: AI HTTP 클라이언트는 post 텍스트 검사와 공유한다. 클라이언트 이동은 PR06(post 전환) 때 adapter/http 로.
@Component
@RequiredArgsConstructor
public class AiImageModerationAdapter implements ImageModerationPort {

    private final AiModerationClient aiModerationClient;

    @Override
    public boolean isNsfw(String fileName, byte[] content) {
        return aiModerationClient.isNsfw(content, fileName);
    }
}
