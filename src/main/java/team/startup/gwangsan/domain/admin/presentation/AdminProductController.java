package team.startup.gwangsan.domain.admin.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.domain.admin.service.DeleteAdminProductService;
import team.startup.gwangsan.domain.admin.service.FindAdminProductsService;
import team.startup.gwangsan.domain.post.entity.constant.Mode;
import team.startup.gwangsan.domain.post.entity.constant.Type;
import team.startup.gwangsan.domain.post.presentation.dto.response.GetProductResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final FindAdminProductsService findAdminProductsService;
    private final DeleteAdminProductService deleteAdminProductService;

    @GetMapping
    public ResponseEntity<List<GetProductResponse>> findProducts(
            @RequestParam(required = false) Type type,
            @RequestParam(required = false) Mode mode,
            @RequestParam(name = "last_id", required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (size < 1 || (lastId != null && lastId < 1)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(findAdminProductsService.execute(type, mode, lastId, size));
    }

    @DeleteMapping("/{product_id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("product_id") Long productId) {
        if (productId < 1) {
            return ResponseEntity.badRequest().build();
        }
        deleteAdminProductService.execute(productId);
        return ResponseEntity.noContent().build();
    }
}
