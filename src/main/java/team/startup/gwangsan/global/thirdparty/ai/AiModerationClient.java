package team.startup.gwangsan.global.thirdparty.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class AiModerationClient {

    private final RestClient restClient;

    public AiModerationClient(AiModerationProperties properties, RestClient.Builder restClientBuilder) {
        if (!properties.isEnabled()) {
            this.restClient = null;
            return;
        }

        RestClient.Builder builder = restClientBuilder.baseUrl(properties.url());

        if (properties.connectTimeout() != null && properties.readTimeout() != null) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(properties.connectTimeout());
            requestFactory.setReadTimeout(properties.readTimeout());
            builder.requestFactory(requestFactory);
        }

        this.restClient = builder.build();
    }

    public boolean containsProfanity(String text) {
        if (restClient == null || text == null || text.isBlank()) {
            return false;
        }

        try {
            PredictResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PredictRequest(text))
                    .retrieve()
                    .body(PredictResponse.class);
            return response != null && response.label() == 1;
        } catch (RestClientException exception) {
            log.warn("AI 비속어 검사에 실패하여 검사를 건너뜁니다.", exception);
            return false;
        }
    }

    public boolean isNsfw(MultipartFile file) {
        if (restClient == null || file == null || file.isEmpty()) {
            return false;
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(file.getBytes(), file.getOriginalFilename()));

            NsfwResponse response = restClient.post()
                    .uri("/nsfw")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(NsfwResponse.class);
            return response != null && response.isNsfw();
        } catch (IOException | RestClientException exception) {
            log.warn("AI 이미지 검사에 실패하여 검사를 건너뜁니다.", exception);
            return false;
        }
    }

    private record PredictRequest(String text) {
    }

    private record PredictResponse(int label) {
    }

    private record NsfwResponse(@JsonProperty("is_nsfw") boolean isNsfw) {
    }

    private static class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
