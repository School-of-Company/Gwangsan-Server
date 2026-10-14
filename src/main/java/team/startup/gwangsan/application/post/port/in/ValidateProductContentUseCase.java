package team.startup.gwangsan.application.post.port.in;

public interface ValidateProductContentUseCase {
    /** 부적절한 내용이면 InappropriateContentException. 트랜잭션 밖에서 생성/수정 전에 호출한다. */
    void execute(String title, String description);
}
