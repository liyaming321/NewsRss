# 2026-07-01 忽略本地任务与设计检查文件

## 实现过程

1. 在根目录 `.gitignore` 中新增本地规划文件忽略规则。
2. 将 `task.md` 和 `design-qa.md` 加入忽略列表，避免上传到 GitHub。

## 验证命令

```bash
rg -n "task.md|design-qa.md" .gitignore
```

## 执行结果

- `.gitignore` 已包含 `task.md` 和 `design-qa.md`。

## 遗留问题

- 如果这两个文件已经被 Git 跟踪，仅加入 `.gitignore` 不会自动停止跟踪，需要执行 `git rm --cached task.md design-qa.md`。
