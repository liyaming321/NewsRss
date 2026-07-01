package com.newsrss.service.rss;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.newsrss.config.RssHttpProperties;
import com.newsrss.service.parser.ParserTemplateConfig;
import com.newsrss.service.parser.ParserTemplateNormalizer;
import com.newsrss.service.parser.RssEntryRawPayloadBuilder;
import com.newsrss.service.parser.TemplateNormalizedArticle;
import java.io.IOException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.List;
import java.util.function.Function;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS Feed 解析器，负责从远端 URL 抓取并解析 RSS/Atom 内容。
 */
@Component
@Profile("db")
public class RssFeedParser {

    private final RssArticleNormalizer articleNormalizer;
    private final RssEntryRawPayloadBuilder rawPayloadBuilder;
    private final ParserTemplateNormalizer templateNormalizer;
    private final RssHttpProperties httpProperties;

    public RssFeedParser(
            RssArticleNormalizer articleNormalizer,
            RssEntryRawPayloadBuilder rawPayloadBuilder,
            ParserTemplateNormalizer templateNormalizer,
            RssHttpProperties httpProperties) {
        this.articleNormalizer = articleNormalizer;
        this.rawPayloadBuilder = rawPayloadBuilder;
        this.templateNormalizer = templateNormalizer;
        this.httpProperties = httpProperties;
    }

    /**
     * 抓取并解析指定订阅地址。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @return 解析后的 Feed 数据
     */
    public ParsedFeed parse(String feedUrl) {
        return parseFeed(feedUrl, "抓取或解析 RSS 失败", syndFeed -> {
            List<ParsedArticleItem> items = syndFeed.getEntries().stream()
                    .map(entry -> articleNormalizer.normalize(feedUrl, entry))
                    .toList();
            return ParsedFeed.defaultParsed(
                    syndFeed.getTitle(),
                    syndFeed.getLink(),
                    syndFeed.getDescription(),
                    syndFeed.getLanguage(),
                    items);
        });
    }

    /**
     * 使用解析模板抓取并解析指定订阅地址。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @param templateConfig 解析模板配置
     * @return 模板解析后的 Feed 数据
     */
    public ParsedFeed parse(String feedUrl, ParserTemplateConfig templateConfig) {
        if (templateConfig == null || !templateConfig.enabled()) {
            return parse(feedUrl);
        }
        return parseFeed(feedUrl, "抓取或模板解析 RSS 失败", syndFeed -> {
            List<TemplateNormalizedArticle> templateArticles = syndFeed.getEntries().stream()
                    .map(rawPayloadBuilder::build)
                    .map(rawPayload -> templateNormalizer.normalize(feedUrl, rawPayload, templateConfig))
                    .toList();
            return ParsedFeed.templateParsed(
                    syndFeed.getTitle(),
                    syndFeed.getLink(),
                    syndFeed.getDescription(),
                    syndFeed.getLanguage(),
                    templateArticles);
        });
    }

    /**
     * 抓取订阅源并返回原始条目字段样本，不执行模板解析和入库。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @param limit 样本条数
     * @return 原始条目样本
     */
    public RawFeedSample sampleRawPayloads(String feedUrl, int limit) {
        return parseFeed(feedUrl, "抓取 RSS 样本失败", syndFeed -> {
            List<com.fasterxml.jackson.databind.JsonNode> payloads = syndFeed.getEntries().stream()
                    .limit(limit)
                    .map(rawPayloadBuilder::build)
                    .toList();
            return new RawFeedSample(
                    syndFeed.getTitle(),
                    syndFeed.getLink(),
                    syndFeed.getDescription(),
                    syndFeed.getLanguage(),
                    payloads);
        });
    }

    /**
     * 打开订阅源并把 Rome Feed 交给调用方转换。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @param errorPrefix 业务错误前缀
     * @param mapper Feed 转换函数
     * @param <T> 转换结果类型
     * @return 转换结果
     */
    private <T> T parseFeed(String feedUrl, String errorPrefix, Function<SyndFeed, T> mapper) {
        try {
            URLConnection connection = openConnection(feedUrl);
            try (XmlReader reader = new XmlReader(connection)) {
                return mapper.apply(new SyndFeedInput().build(reader));
            }
        } catch (SocketTimeoutException exception) {
            throw new RssFetchException(timeoutMessage(errorPrefix, feedUrl), exception);
        } catch (Exception exception) {
            throw new RssFetchException(errorPrefix + "：" + readableMessage(exception), exception);
        }
    }

    /**
     * 创建带超时和 User-Agent 的远端连接。
     *
     * @param feedUrl RSS 或 Atom 订阅地址
     * @return 远端连接
     * @throws IOException 连接创建失败
     */
    private URLConnection openConnection(String feedUrl) throws IOException {
        URLConnection connection = URI.create(feedUrl).toURL().openConnection();
        connection.setConnectTimeout(Math.toIntExact(httpProperties.resolvedConnectTimeout().toMillis()));
        connection.setReadTimeout(Math.toIntExact(httpProperties.resolvedReadTimeout().toMillis()));
        connection.setRequestProperty("User-Agent", httpProperties.resolvedUserAgent());
        connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*");
        return connection;
    }

    /**
     * 构造网络超时错误提示。
     *
     * @param errorPrefix 业务错误前缀
     * @param feedUrl RSS 或 Atom 订阅地址
     * @return 可读超时提示
     */
    private String timeoutMessage(String errorPrefix, String feedUrl) {
        return "%s：远端 RSS 响应超时，请稍后重试或在配置中调大 newsrss.rss.http.read-timeout-seconds。地址：%s，当前读取超时：%d 秒"
                .formatted(errorPrefix, feedUrl, httpProperties.resolvedReadTimeout().toSeconds());
    }

    /**
     * 获取可读异常消息。
     *
     * @param exception 原始异常
     * @return 可读异常消息
     */
    private String readableMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * 原始订阅源样本。
     *
     * @param title 订阅源标题
     * @param siteUrl 站点地址
     * @param description 订阅源说明
     * @param language 语言代码
     * @param payloads 原始条目字段样本
     */
    public record RawFeedSample(
            String title,
            String siteUrl,
            String description,
            String language,
            List<com.fasterxml.jackson.databind.JsonNode> payloads) {
    }
}
