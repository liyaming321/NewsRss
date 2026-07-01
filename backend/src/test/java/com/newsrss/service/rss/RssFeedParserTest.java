package com.newsrss.service.rss;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsrss.config.RssHttpProperties;
import com.newsrss.service.parser.ParserTemplateNormalizer;
import com.newsrss.service.parser.RssEntryRawPayloadBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RssFeedParserTest {

    private HttpServer server;

    /**
     * 关闭测试 HTTP 服务。
     */
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * 验证远端读取超时时会返回中文业务提示，而不是裸露底层 Read timed out。
     */
    @Test
    void shouldReportReadableMessageWhenReadTimeout() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/slow.xml", this::slowResponse);
        server.start();
        String feedUrl = "http://127.0.0.1:%d/slow.xml".formatted(server.getAddress().getPort());
        RssHttpProperties properties = new RssHttpProperties();
        properties.setConnectTimeoutSeconds(1);
        properties.setReadTimeoutSeconds(1);
        ObjectMapper objectMapper = new ObjectMapper();
        RssFeedParser parser = new RssFeedParser(
                new RssArticleNormalizer(objectMapper, new RssContentCleaner(), new RssFingerprintGenerator()),
                new RssEntryRawPayloadBuilder(objectMapper),
                new ParserTemplateNormalizer(objectMapper, new RssContentCleaner(), new RssFingerprintGenerator()),
                properties);

        assertThatThrownBy(() -> parser.parse(feedUrl))
                .isInstanceOf(RssFetchException.class)
                .hasMessageContaining("远端 RSS 响应超时")
                .hasMessageContaining("newsrss.rss.http.read-timeout-seconds")
                .hasMessageContaining("当前读取超时：1 秒");
    }

    /**
     * 模拟建立连接后迟迟不返回响应体的 RSS 服务。
     *
     * @param exchange HTTP 交换上下文
     */
    private void slowResponse(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(2_500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        byte[] body = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0">
                  <channel>
                    <title>Slow Feed</title>
                    <link>https://example.com</link>
                    <description>slow</description>
                  </channel>
                </rss>
                """.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
