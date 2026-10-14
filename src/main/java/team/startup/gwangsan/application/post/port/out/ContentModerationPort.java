package team.startup.gwangsan.application.post.port.out;

public interface ContentModerationPort {
    boolean containsProfanity(String text);
}
