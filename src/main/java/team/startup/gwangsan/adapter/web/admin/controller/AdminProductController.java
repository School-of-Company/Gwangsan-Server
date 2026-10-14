package team.startup.gwangsan.adapter.web.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.startup.gwangsan.application.admin.data.AdminProductSearch;
import team.startup.gwangsan.application.admin.port.in.DeleteAdminProductUseCase;
import team.startup.gwangsan.application.admin.port.in.FindAdminProductsQuery;
import team.startup.gwangsan.domain.post.model.Mode;
import team.startup.gwangsan.domain.post.model.Type;
import team.startup.gwangsan.adapter.web.post.dto.GetProductResponse;

import java.util.List;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final FindAdminProductsQuery findAdminProductsQuery;
    private final DeleteAdminProductUseCase deleteAdminProductUseCase;

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
        AdminProductSearch search = new AdminProductSearch(
                type == null ? null : type.name(), mode == null ? null : mode.name(), lastId, size);
        return ResponseEntity.ok(findAdminProductsQuery.query(search).stream()
                .map(AdminWebMapper::toResponse)
                .toList());
    }

    @DeleteMapping("/{product_id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("product_id") Long productId) {
        if (productId < 1) {
            return ResponseEntity.badRequest().build();
        }
        deleteAdminProductUseCase.execute(productId);
        return ResponseEntity.noContent().build();
    }
}
