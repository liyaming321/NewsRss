package com.newsrss.config;

import com.newsrss.dto.RssFetchResult;
import com.newsrss.service.rss.RssFeedFetchService;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS 抓取冒烟验证运行器，用于在命令行环境批量抓取指定订阅源。
 */
@Component
@Profile("db")
public class RssFetchSmokeRunner implements ApplicationRunner {

    private final RssFetchSmokeProperties properties;
    private final RssFeedFetchService fetchService;
    private final ConfigurableApplicationContext applicationContext;

    public RssFetchSmokeRunner(
            RssFetchSmokeProperties properties,
            RssFeedFetchService fetchService,
            ConfigurableApplicationContext applicationContext) {
        this.properties = properties;
        this.fetchService = fetchService;
        this.applicationContext = applicationContext;
    }

    /**
     * 执行冒烟抓取，并在完成后退出应用。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        List<RssFetchResult> results = properties.getUrls().stream()
                .map(fetchService::fetch)
                .toList();
        results.forEach(result -> System.out.printf(
                "RSS_FETCH_RESULT feedUrl=%s success=%s fetched=%d new=%d duplicate=%d failed=%d error=%s%n",
                result.feedUrl(),
                result.success(),
                result.fetchedCount(),
                result.newCount(),
                result.duplicateCount(),
                result.failedCount(),
                result.errorMessage()));
        SpringApplication.exit(applicationContext, () -> results.stream().allMatch(RssFetchResult::success) ? 0 : 1);
    }
}
