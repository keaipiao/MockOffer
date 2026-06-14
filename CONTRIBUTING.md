# 贡献指南 / Contributing

感谢参与 **模考Offer**！本指南帮你快速上手。Thanks for contributing!

## 本地开发 / Local development

前置 / Prerequisites：[Docker](https://www.docker.com/) + Docker Compose（仅本机直跑前/后端时才另需 Node 22 / Java 21）。

一键启停 / One-command up & down：

```bash
make up        # macOS / Linux —— 启动全部服务
make down      # 停止
make logs      # 看日志
make ps        # 看状态
```

Windows（PowerShell）：

```powershell
./scripts/dev.ps1 up      # down | restart | logs | ps | build | clean
```

启动后访问 http://localhost:3000 。

## 提交规范 / Commit messages

中文 + 类型前缀 / Chinese + type prefix：`feat` / `fix` / `refactor` / `test` / `docs` / `chore`。

例：`feat: 邮箱验证码登录`

## 分支与 PR / Branch & PR

- 从 `main` 切功能分支：`feat/xxx`、`fix/xxx`。
- 提 PR 前确保 CI 全绿（前端 `npm run lint && npm run build`，后端 `./mvnw verify`）。
- 按 PR 模板填写变更说明与自测项。

## 代码风格 / Code style

- 统一遵循根目录 [`.editorconfig`](.editorconfig)。
- 前端走 ESLint；后端遵循 Java 标准风格。
- **中文优先**：注释与项目文档用中文。

## 开发流程 / Workflow

本项目用一套标准化流程开发（见 [`docs/开发流程SOP.md`](docs/开发流程SOP.md)），进度由 [灵计一动](https://lingji1dong.com) 看板管理。提 PR 无需接入看板，按上面的提交/分支规范即可。
