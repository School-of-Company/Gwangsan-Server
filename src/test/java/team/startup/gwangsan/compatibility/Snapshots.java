package team.startup.gwangsan.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 호환성 기준 스냅샷 비교. 기준 파일은 baseline develop 474a2cde 에서 생성했다.
 * 기준을 바꾸는 것은 계약 변경이므로 사용자 승인 없이 재생성하지 않는다.
 * 승인된 변경일 때만 환경변수 COMPAT_SNAPSHOT_UPDATE=true 로 src/test/resources 의 파일을 다시 쓴다.
 */
final class Snapshots {

    private Snapshots() {
    }

    static void assertMatches(String resource, String actual) {
        if ("true".equals(System.getenv("COMPAT_SNAPSHOT_UPDATE"))) {
            write(resource, actual);
            return;
        }
        assertThat(actual).as("%s 와 다르다. 의도한 계약 변경이 아니면 이동/이름 변경을 되돌린다", resource)
                .isEqualTo(read(resource));
    }

    private static String read(String resource) {
        try (InputStream in = Snapshots.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " 없음 (기준 스냅샷 누락은 실패로 처리)");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(String resource, String content) {
        try {
            Path path = Path.of("src/test/resources").resolve(resource);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
