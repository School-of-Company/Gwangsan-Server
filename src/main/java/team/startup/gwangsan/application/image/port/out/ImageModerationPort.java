package team.startup.gwangsan.application.image.port.out;

public interface ImageModerationPort {
    /** 검사 불가(비활성·빈 파일·외부 실패)는 false 다. */
    boolean isNsfw(String fileName, byte[] content);
}
