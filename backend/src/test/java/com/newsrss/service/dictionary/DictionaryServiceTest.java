package com.newsrss.service.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.newsrss.domain.entity.SystemDictionary;
import com.newsrss.dto.dictionary.DictionaryRequest;
import com.newsrss.repository.SystemDictionaryRepository;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DictionaryServiceTest {

    private final SystemDictionaryRepository dictionaryRepository = Mockito.mock(SystemDictionaryRepository.class);
    private final DictionaryService dictionaryService = new DictionaryService(dictionaryRepository);

    /**
     * 验证可以创建订阅源分类字典项。
     */
    @Test
    void shouldCreateFeedCategoryItem() throws Exception {
        when(dictionaryRepository.existsByDictTypeAndItemCode(DictionaryService.FEED_CATEGORY, "Tech")).thenReturn(false);
        when(dictionaryRepository.save(any(SystemDictionary.class))).thenAnswer(invocation -> {
            SystemDictionary dictionary = invocation.getArgument(0);
            setId(dictionary, 1L);
            return dictionary;
        });
        DictionaryRequest request = new DictionaryRequest("Tech", "技术", "技术资讯", 10, true);

        var response = dictionaryService.createItem(DictionaryService.FEED_CATEGORY, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.dictType()).isEqualTo(DictionaryService.FEED_CATEGORY);
        assertThat(response.itemCode()).isEqualTo("Tech");
        assertThat(response.itemLabel()).isEqualTo("技术");
        assertThat(response.enabled()).isTrue();
    }

    /**
     * 验证重复编码会被拒绝。
     */
    @Test
    void shouldRejectDuplicateItemCode() {
        when(dictionaryRepository.existsByDictTypeAndItemCode(DictionaryService.FEED_CATEGORY, "Tech")).thenReturn(true);
        DictionaryRequest request = new DictionaryRequest("Tech", "技术", null, 10, true);

        assertThatThrownBy(() -> dictionaryService.createItem(DictionaryService.FEED_CATEGORY, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("字典项编码已存在");
    }

    /**
     * 验证订阅源分类必须存在且启用。
     */
    @Test
    void shouldRejectDisabledFeedCategory() {
        SystemDictionary dictionary = SystemDictionary.create(
                DictionaryService.FEED_CATEGORY,
                "Tech",
                "技术",
                null,
                10,
                false,
                OffsetDateTime.now(ZoneOffset.UTC));
        when(dictionaryRepository.findByDictTypeAndItemCode(DictionaryService.FEED_CATEGORY, "Tech"))
                .thenReturn(Optional.of(dictionary));

        assertThatThrownBy(() -> dictionaryService.validateEnabledFeedCategory("Tech"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订阅源分类已停用");
    }

    /**
     * 设置测试实体主键。
     *
     * @param dictionary 字典实体
     * @param id 主键
     */
    private void setId(SystemDictionary dictionary, Long id) throws Exception {
        Field idField = SystemDictionary.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dictionary, id);
    }
}
