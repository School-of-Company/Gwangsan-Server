package team.startup.gwangsan.domain.post.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.request.CreateProductRequest;
import team.startup.gwangsan.domain.post.presentation.dto.request.PatchProductRequest;
import team.startup.gwangsan.domain.post.presentation.dto.request.RequestTradeCompleteRequest;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductByIdResponse;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;
import team.startup.gwangsan.domain.post.service.*;

import java.util.List;

@RestController
@RequestMapping("api/post")
@RequiredArgsConstructor
public class PostController {

    private final CreateProductService postProductService;
    private final FindProductByIdService findProductByIdService;
    private final FindProductsByTypeAndModeService findProductsByTypeAndModeService;
    private final FindProductByCurrentUserAndTypeAndModeService findProductByCurrentUserAndTypeAndModeService;
    private final UpdateProductService updateProductService;
    private final DeleteProductByIdService deleteProductByIdService;
    private final FindProductsByMemberIdService findProductsByMemberIdService;
    private final RequestTradeCompleteService requestTradeCompleteService;
    private final ReservationProductService reservationProductService;
    private final DeleteReservationProductService deleteReservationProductService;

    @PostMapping
    public ResponseEntity<Void> createProduct(@RequestBody @Valid CreateProductRequest request) {
        postProductService.execute(
                request.type(),
                request.mode(),
                request.title(),
                request.content(),
                request.gwangsan(),
                request.imageIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{post_id}")
    public ResponseEntity<GetProductByIdResponse> findProductById(@PathVariable("post_id") Long postId) {
        GetProductByIdResponse response = findProductByIdService.execute(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> findProductByTypeAndMode(
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode
    ) {
        List<GetProductResponse> responses = findProductsByTypeAndModeService.execute(type, mode);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/current")
    public ResponseEntity<List<GetProductResponse>> findCurrentProduct(
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode
    ) {
        List<GetProductResponse> responses = findProductByCurrentUserAndTypeAndModeService.execute(type, mode);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{post_id}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable("post_id") Long postId,
            @RequestBody @Valid PatchProductRequest request
    ) {
        updateProductService.execute(
                postId,
                request.type(),
                request.mode(),
                request.title(),
                request.content(),
                request.gwangsan(),
                request.imageIds()
        );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{post_id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("post_id") Long postId) {
        deleteProductByIdService.execute(postId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/member/{member_id}")
    public ResponseEntity<List<GetProductResponse>> findByMemberID(
            @PathVariable("member_id") Long memberId,
            @RequestParam(name = "type", required = false) Type type,
            @RequestParam(name = "mode", required = false) Mode mode
    ) {
        List<GetProductResponse> responses = findProductsByMemberIdService.execute(memberId, type, mode);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/trade")
    public ResponseEntity<Void> tradeRequest(@RequestBody @Valid RequestTradeCompleteRequest request) {
        requestTradeCompleteService.execute(request.productId(), request.otherMemberId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PatchMapping("/reservation/{product_id}")
    public ResponseEntity<Void> reservation(@PathVariable("product_id") Long productId) {
        reservationProductService.execute(productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reservation/{product_id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable("product_id") Long productId) {
        deleteReservationProductService.execute(productId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
