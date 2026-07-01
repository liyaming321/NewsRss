# 2026-07-01 开源 README 与忽略规则整理

## 实现过程

1. 将已有 Playwright 截图复制到 `docs/assets/screenshots/`，用于 README 展示驾驶舱、阅读页、解析模板和系统设置。
2. 新增根目录 `.gitignore`，统一忽略本地依赖、构建产物、运行日志、Playwright 临时文件、IDE 文件和敏感配置。
3. 将 `backend/src/main/resources/application-db.yml` 和 `backend/src/main/resources/application-dev.yml` 加入忽略规则，避免数据库配置和本地密钥提交到 GitHub。
4. 新增 `backend/src/main/resources/application-db.example.yml`，提供可提交的数据库配置模板，不包含真实密钥。
5. 重写 README，补充项目介绍、功能特性、截图、技术栈、本地开发、单 Jar 部署、数据库迁移、验证命令和开源前检查说明。

## 验证命令

```bash
find docs/assets/screenshots -type f -maxdepth 1 -print | sort
```

```bash
rg -n "backend/src/main/resources/application-db.yml|backend/target|frontend/dist|node_modules|.playwright-cli" .gitignore
```

```bash
rg -n "sk-[A-Za-z0-9_-]+" README.md backend/src/main/resources/application-db.example.yml .gitignore docs/change-log-2026-07-01-open-source-readme.md || true
```

## 执行结果

- README 已包含 4 张项目截图，引用路径均在 `docs/assets/screenshots/` 下。
- 根目录 `.gitignore` 已覆盖后端 target、前端 dist、node_modules、运行日志、Playwright 临时目录和本地敏感配置。
- 示例数据库配置文件不包含真实 DeepSeek API Key。
- 未修改用户本地 `backend/src/main/resources/application-db.yml` 内容。

## 遗留问题

- 当前项目尚未补充正式 `LICENSE` 文件，开源前建议选择 MIT 或 Apache-2.0。
- 如果 `application-db.yml` 已经被 Git 跟踪，仅加入 `.gitignore` 不会自动停止跟踪，需要在 Git 仓库中执行 `git rm --cached backend/src/main/resources/application-db.yml`。
