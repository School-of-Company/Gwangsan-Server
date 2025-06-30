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

    @PostMapping
    public ResponseEntity<Void> createProduct(@RequestBody @Valid CreateProductRequest request) {
        postProductService.execute(
                request.type(),
                request.mode(),
                request.title(),
                request.description(),
                request.gwangsan(),
                request.imageIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{post_id}")
    public ResponseEntity<GetProductResponse> findProductById(@PathVariable("post_id") Long postId) {
        GetProductResponse response = findProductByIdService.execute(postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> findProductByTypeAndMode(
            @RequestParam Type type,
            @RequestParam Mode mode
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
                request.description(),
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
}
