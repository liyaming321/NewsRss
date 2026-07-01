package com.newsrss.service.rss;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS 文章指纹生成器，用于根据稳定字段生成去重指纹。
 */
@Component
@Profile("db")
public class RssFingerprintGenerator {

    /**
     * 根据订阅源和文章关键字段生成 SHA-256 指纹。
     *
     * @param feedUrl 订阅地址
     * @param guid RSS 条目 GUID
     * @param articleUrl 文章链接
     * @param title 文章标题
     * @param publishedAtText 发布时间文本
     * @return 十六进制 SHA-256 指纹
     */
    public String generate(
            String feedUrl,
            String guid,
            String articleUrl,
            String title,
            String publishedAtText) {
        String source = String.join(
                "|",
                safe(feedUrl),
                safe(guid),
                safe(articleUrl),
                safe(title),
                safe(publishedAtText));
        return sha256(source);
    }

    private String safe(String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * 对输入文本生成 SHA-256 十六进制摘要。
     *
     * @param source 输入文本
     * @return 十六进制摘要
     */
    private String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashBytes.length * 2);
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256 指纹算法", exception);
        }
    }
}
