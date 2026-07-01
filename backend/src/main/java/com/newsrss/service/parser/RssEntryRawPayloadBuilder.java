package com.newsrss.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndPerson;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RSS 条目原始字段构建器，将 Rome 条目整理成模板解析可查询的 JSON 字段树。
 */
@Component
@Profile("db")
public class RssEntryRawPayloadBuilder {

    private final ObjectMapper objectMapper;

    public RssEntryRawPayloadBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建原始条目字段树。
     *
     * @param entry Rome 条目
     * @return 原始条目字段树
     */
    public JsonNode build(SyndEntry entry) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("uri", entry.getUri());
        payload.put("title", entry.getTitle());
        payload.put("link", entry.getLink());
        payload.put("author", entry.getAuthor());
        payload.put("comments", entry.getComments());
        payload.put("publishedDate", entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant().toString());
        payload.put("updatedDate", entry.getUpdatedDate() == null ? null : entry.getUpdatedDate().toInstant().toString());
        payload.set("description", contentNode(entry.getDescription()));
        payload.set("contents", contentsNode(entry.getContents()));
        payload.set("links", linksNode(entry.getLinks()));
        payload.set("authors", peopleNode(entry.getAuthors()));
        payload.set("contributors", peopleNode(entry.getContributors()));
        payload.set("enclosures", enclosuresNode(entry.getEnclosures()));
        payload.set("categories", categoriesNode(entry.getCategories()));
        payload.set("foreignMarkup", foreignMarkupNode(entry.getForeignMarkup()));
        return payload;
    }

    /**
     * 将 Rome 内容对象转换为 JSON 节点。
     *
     * @param content Rome 内容对象
     * @return 内容 JSON 节点
     */
    private JsonNode contentNode(SyndContent content) {
        if (content == null) {
            return objectMapper.nullNode();
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("type", content.getType());
        node.put("mode", content.getMode());
        node.put("value", content.getValue());
        return node;
    }

    /**
     * 将正文内容列表转换为 JSON 数组。
     *
     * @param contents Rome 正文内容列表
     * @return 内容 JSON 数组
     */
    private ArrayNode contentsNode(List<SyndContent> contents) {
        ArrayNode nodes = objectMapper.createArrayNode();
        contents.forEach(content -> nodes.add(contentNode(content)));
        return nodes;
    }

    /**
     * 将链接列表转换为 JSON 数组。
     *
     * @param links Rome 链接列表
     * @return 链接 JSON 数组
     */
    private ArrayNode linksNode(List<SyndLink> links) {
        ArrayNode nodes = objectMapper.createArrayNode();
        links.forEach(link -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("rel", link.getRel());
            node.put("type", link.getType());
            node.put("href", link.getHref());
            node.put("title", link.getTitle());
            node.put("hreflang", link.getHreflang());
            node.put("length", link.getLength());
            nodes.add(node);
        });
        return nodes;
    }

    /**
     * 将作者或贡献者列表转换为 JSON 数组。
     *
     * @param people Rome 人员列表
     * @return 人员 JSON 数组
     */
    private ArrayNode peopleNode(List<SyndPerson> people) {
        ArrayNode nodes = objectMapper.createArrayNode();
        people.forEach(person -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", person.getName());
            node.put("uri", person.getUri());
            node.put("email", person.getEmail());
            nodes.add(node);
        });
        return nodes;
    }

    /**
     * 将附件列表转换为 JSON 数组。
     *
     * @param enclosures Rome 附件列表
     * @return 附件 JSON 数组
     */
    private ArrayNode enclosuresNode(List<SyndEnclosure> enclosures) {
        ArrayNode nodes = objectMapper.createArrayNode();
        enclosures.forEach(enclosure -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", enclosure.getType());
            node.put("url", enclosure.getUrl());
            node.put("length", enclosure.getLength());
            nodes.add(node);
        });
        return nodes;
    }

    /**
     * 将分类列表转换为 JSON 数组。
     *
     * @param categories Rome 分类列表
     * @return 分类 JSON 数组
     */
    private ArrayNode categoriesNode(List<SyndCategory> categories) {
        ArrayNode nodes = objectMapper.createArrayNode();
        categories.forEach(category -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", category.getName());
            node.put("label", category.getLabel());
            node.put("taxonomyUri", category.getTaxonomyUri());
            nodes.add(node);
        });
        return nodes;
    }

    /**
     * 将扩展命名空间字段转换为按标签名索引的 JSON 对象。
     *
     * @param elements 扩展字段列表
     * @return 扩展字段 JSON 对象
     */
    private ObjectNode foreignMarkupNode(List<Element> elements) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        elements.forEach(element -> addForeignMarkup(rootNode, element));
        return rootNode;
    }

    /**
     * 写入单个扩展字段。
     *
     * @param rootNode 扩展字段根节点
     * @param element JDOM 元素
     */
    private void addForeignMarkup(ObjectNode rootNode, Element element) {
        String key = element.getName();
        ObjectNode elementNode = objectMapper.createObjectNode();
        elementNode.put("text", element.getTextNormalize());
        elementNode.put("value", element.getValue());
        ObjectNode attributesNode = objectMapper.createObjectNode();
        element.getAttributes().forEach(attribute -> putAttribute(attributesNode, attribute));
        elementNode.set("attributes", attributesNode);
        ArrayNode childrenNode = objectMapper.createArrayNode();
        element.getChildren().forEach(child -> {
            ObjectNode childNode = objectMapper.createObjectNode();
            childNode.put("name", child.getName());
            childNode.put("text", child.getTextNormalize());
            childNode.put("value", child.getValue());
            childrenNode.add(childNode);
        });
        elementNode.set("children", childrenNode);
        rootNode.set(key, elementNode);
    }

    /**
     * 写入扩展字段属性。
     *
     * @param attributesNode 属性节点
     * @param attribute JDOM 属性
     */
    private void putAttribute(ObjectNode attributesNode, Attribute attribute) {
        attributesNode.put(attribute.getName(), attribute.getValue());
    }
}
