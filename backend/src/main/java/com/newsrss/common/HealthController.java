package com.newsrss.common;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供应用基础健康信息，方便前端和部署脚本确认后端服务是否已经启动。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;
    private final String applicationVersion;

    public HealthController(
            @Value("${newsrss.app.display-name}") String applicationName,
            @Value("${newsrss.app.version}") String applicationVersion) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    /**
     * 返回后端服务当前运行状态。
     *
     * @return 后端服务基础状态信息
     */
    @GetMapping
    public HealthResponse getHealth() {
        return new HealthResponse(applicationName, applicationVersion, "UP", Instant.now());
    }
}
