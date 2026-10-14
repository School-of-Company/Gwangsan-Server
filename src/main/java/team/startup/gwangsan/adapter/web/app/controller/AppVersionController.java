package team.startup.gwangsan.adapter.web.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.startup.gwangsan.adapter.web.app.dto.AppVersionResponse;
import team.startup.gwangsan.application.app.data.AppVersionResult;
import team.startup.gwangsan.application.app.port.in.FindAppVersionQuery;

@RestController
@RequiredArgsConstructor
public class AppVersionController {

    private final FindAppVersionQuery findAppVersionQuery;

    @GetMapping("/api/app/version")
    public ResponseEntity<AppVersionResponse> findVersion(
            @RequestParam(required = false) String platform) {
        if (!"ios".equals(platform) && !"android".equals(platform)) {
            return ResponseEntity.badRequest().build();
        }
        AppVersionResult result = findAppVersionQuery.query(platform);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AppVersionResponse(result.latestVersion(), result.minimumVersion()));
    }
}
