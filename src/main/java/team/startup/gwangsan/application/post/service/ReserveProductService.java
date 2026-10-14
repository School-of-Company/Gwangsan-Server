package team.startup.gwangsan.application.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.startup.gwangsan.application.post.data.ChatRoomView;
import team.startup.gwangsan.application.post.data.ReserveProductCommand;
import team.startup.gwangsan.application.post.port.in.ReserveProductUseCase;
import team.startup.gwangsan.application.post.port.out.ProductChatPort;
import team.startup.gwangsan.application.post.port.out.ProductPort;
import team.startup.gwangsan.application.post.port.out.ReservationPort;
import team.startup.gwangsan.application.post.port.out.TradeStatusPort;
import team.startup.gwangsan.domain.post.exception.NotFoundProductException;
import team.startup.gwangsan.domain.post.exception.ReservationChatRoomNotFoundException;
import team.startup.gwangsan.domain.post.model.Product;
import team.startup.gwangsan.domain.post.model.ProductReservation;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReserveProductService implements ReserveProductUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final ProductPort productPort;
    private final ReservationPort reservationPort;
    private final ProductChatPort chatPort;
    private final TradeStatusPort tradeStatusPort;

    @Override
    @Transactional
    public void execute(ReserveProductCommand command) {
        // 상품 행 잠금으로 작성자 삭제·거래 요청과 직렬화한다.
        Product product = productPort.lockActive(command.productId())
                .orElseThrow(NotFoundProductException::new);
        product.reserve(command.actorId());

        ChatRoomView room = chatPort.findRoom(command.roomId())
                .filter(found -> found.productId().equals(command.productId()))
                .orElseThrow(ReservationChatRoomNotFoundException::new);
        ProductReservation reservation = ProductReservation.reserve(product.getId(), room.otherOf(command.actorId()),
                command.scheduledAt(), command.placeName(), command.address(), command.latitude(),
                command.longitude(), LocalDateTime.now(SEOUL));

        reservationPort.save(reservation);
        productPort.save(product);
        tradeStatusPort.publishChanged(room.roomId(), product.getId(), room.buyerId(), room.sellerId());
    }
}
