alter table rss_parser_template
    add column custom_field_mapping jsonb not null default '{}'::jsonb;

comment on column rss_parser_template.custom_field_mapping is '自定义字段映射配置，保存标准字段之外的源特有字段路径';

alter table rss_article
    add column custom_fields jsonb not null default '{}'::jsonb;

comment on column rss_article.custom_fields is '自定义解析字段，保存标准字段之外的源特有扩展信息';
