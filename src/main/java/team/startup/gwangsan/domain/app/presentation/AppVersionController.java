package team.startup.gwangsan.domain.app.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.domain.app.presentation.dto.response.AppVersionResponse;
import team.startup.gwangsan.domain.app.service.FindAppVersionService;

@RestController
@RequiredArgsConstructor
public class AppVersionController {
    private final FindAppVersionService service;

    @GetMapping("/api/app/version")
    public ResponseEntity<AppVersionResponse> findVersion(
            @RequestParam(required = false) String platform) {
        if (!"ios".equals(platform) && !"android".equals(platform)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.execute(platform));
    }
}
