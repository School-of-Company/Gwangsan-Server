package team.startup.gwangsan.application.post.port.in;

/** 이미지 삭제 시 게시글-이미지 연결 행을 지운다(image 업무 소비). 연결이 없으면 아무 일도 하지 않는다. */
public interface UnlinkProductImageUseCase {

    void unlink(Long productId, Long imageId);
}
