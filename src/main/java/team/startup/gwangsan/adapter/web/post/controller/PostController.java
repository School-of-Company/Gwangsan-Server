package team.startup.gwangsan.adapter.web.post.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.adapter.web.post.dto.CreateProductRequest;
import team.startup.gwangsan.adapter.web.post.dto.GetProductByIdResponse;
import team.startup.gwangsan.adapter.web.post.dto.GetProductResponse;
import team.startup.gwangsan.adapter.web.post.dto.PatchProductRequest;
import team.startup.gwangsan.adapter.web.post.dto.ReservationRequest;
import team.startup.gwangsan.application.post.data.CreateProductCommand;
import team.startup.gwangsan.application.post.data.MemberProductsCriteria;
import team.startup.gwangsan.application.post.data.ReserveProductCommand;
import team.startup.gwangsan.application.post.data.UpdateProductCommand;
import team.startup.gwangsan.application.post.port.in.CancelReservationUseCase;
import team.startup.gwangsan.application.post.port.in.CreateProductUseCase;
import team.startup.gwangsan.application.post.port.in.DeleteProductUseCase;
import team.startup.gwangsan.application.post.port.in.MemberProductsQuery;
import team.startup.gwangsan.application.post.port.in.PlaceProductsQuery;
import team.startup.gwangsan.application.post.port.in.ProductDetailQuery;
import team.startup.gwangsan.application.post.port.in.ReserveProductUseCase;
import team.startup.gwangsan.application.post.port.in.UpdateProductUseCase;
import team.startup.gwangsan.application.post.port.in.ValidateProductContentUseCase;
import team.startup.gwangsan.domain.common.ActorContext;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;

import java.util.List;

@RestController
@RequestMapping("api/post")
@RequiredArgsConstructor
public class PostController {

    private final ValidateProductContentUseCase validateProductContentUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final ProductDetailQuery productDetailQuery;
    private final PlaceProductsQuery placeProductsQuery;
    private final MemberProductsQuery memberProductsQuery;
    private final ReserveProductUseCase reserveProductUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    @PostMapping
    public ResponseEntity<Void> createProduct(ActorContext actor, @RequestBody @Valid CreateProductRequest request) {
        validateProductContentUseCase.execute(request.title(), request.content());
        createProductUseCase.execute(new CreateProductCommand(actor.memberId(), request.type(), request.mode(),
                request.title(), request.content(), request.gwangsan(), request.imageIds()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{post_id}")
    public ResponseEntity<GetProductByIdResponse> findProductById(ActorContext actor, @PathVariable("post_id") Long postId) {
        return ResponseEntity.ok(GetProductByIdResponse.from(productDetailQuery.query(actor.memberId(), postId)));
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> findProductByTypeAndMode(
            ActorContext actor,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode
    ) {
        return ResponseEntity.ok(GetProductResponse.from(placeProductsQuery.query(actor.memberId(), type, mode)));
    }

    @GetMapping("/current")
    public ResponseEntity<List<GetProductResponse>> findCurrentProduct(
            ActorContext actor,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "completed", required = false) Boolean completed
    ) {
        if (isInvalidPage(lastId, size, completed)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(GetProductResponse.from(memberProductsQuery.query(
                new MemberProductsCriteria(actor.memberId(), type, mode, lastId, size, completed))));
    }

    @PatchMapping("/{post_id}")
    public ResponseEntity<Void> updateProduct(
            ActorContext actor,
            @PathVariable("post_id") Long postId,
            @RequestBody @Valid PatchProductRequest request
    ) {
        validateProductContentUseCase.execute(request.title(), request.content());
        updateProductUseCase.execute(new UpdateProductCommand(actor.memberId(), postId, request.type(), request.mode(),
                request.title(), request.content(), request.gwangsan(), request.imageIds()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{post_id}")
    public ResponseEntity<Void> deleteProduct(ActorContext actor, @PathVariable("post_id") Long postId) {
        deleteProductUseCase.execute(actor.memberId(), postId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/member/{member_id}")
    public ResponseEntity<List<GetProductResponse>> findByMemberID(
            @PathVariable("member_id") Long memberId,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "completed", required = false) Boolean completed
    ) {
        if (isInvalidPage(lastId, size, completed)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(GetProductResponse.from(memberProductsQuery.query(
                new MemberProductsCriteria(memberId, type, mode, lastId, size, completed))));
    }

    @PatchMapping("/reservation/{product_id}")
    public ResponseEntity<Void> reservation(
            ActorContext actor,
            @PathVariable("product_id") Long productId,
            @RequestBody @Valid ReservationRequest request
    ) {
        reserveProductUseCase.execute(new ReserveProductCommand(actor.memberId(), productId, request.roomId(),
                request.scheduledAt(), request.placeName(), request.address(), request.latitude(), request.longitude()));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reservation/{product_id}")
    public ResponseEntity<Void> deleteReservation(ActorContext actor, @PathVariable("product_id") Long productId) {
        cancelReservationUseCase.execute(actor.memberId(), productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // 기존 계약: size 없이 last_id/completed 금지, size 1~100, last_id 1 이상. 위반은 빈 body 400.
    private static boolean isInvalidPage(Long lastId, Integer size, Boolean completed) {
        return (size == null && (lastId != null || completed != null))
                || (size != null && (size < 1 || size > 100))
                || (lastId != null && lastId < 1);
    }
}
