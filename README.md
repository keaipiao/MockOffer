<div align="center">

# 模考Offer · MockOffer

**面向求职者的全流程 AI 闭环平台**

[![CI](https://github.com/keaipiao/MockOffer/actions/workflows/ci.yml/badge.svg)](https://github.com/keaipiao/MockOffer/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Next.js](https://img.shields.io/badge/Next.js-16-black)](https://nextjs.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)](https://spring.io)
[![Powered by LingJi](https://img.shields.io/badge/Powered%20by-LingJi%20灵计一动-5B6CFF)](https://lingji1dong.com)

[官网 mockoffer.com](https://mockoffer.com) · **中文** | [English](README.en.md)

</div>

---

**模考Offer** 是一个开源的、面向求职者的全流程 AI 闭环平台：从简历到面试，一站式陪你拿到 Offer。

官方托管站 [mockoffer.com](https://mockoffer.com) 基础功能免费，高级 AI 功能收费以维持项目运转；代码完全开源，可自托管。

## ✨ 功能

- 📧 邮箱验证码无密码登录 + GitHub 快捷登录
- 📝 模板化简历制作：选模板、表单填写、模块显隐/自定义、AI 优化、实时预览、导出
- 🎯 岗位录入与人岗匹配：AI 对「简历 × 目标岗位」打分、指出不足、一键修改
- 🎤 AI 模拟面试：按真实面试流程多轮动态提问，每轮实时打分
- 📊 AI 面试报告：总分 + 分项评分 + 优点 + 可优化点 + 对话回顾

## 🧱 技术栈

前端 Next.js 16 · 后端 Spring Boot 4.1 / Java 21 · PostgreSQL 18 · Redis · MinIO · DeepSeek（经 Spring AI）· Docker Compose

## 🚀 快速开始

前置：安装 [Docker](https://www.docker.com/) 与 Docker Compose。

```bash
git clone https://github.com/keaipiao/MockOffer.git
cd MockOffer
make up                 # macOS / Linux
./scripts/dev.ps1 up    # Windows
```

启动后会打印各服务地址（前端 http://localhost:3000、后端 http://localhost:8080、MinIO 控制台 http://localhost:9001）。
停止：`make down` 或 `./scripts/dev.ps1 down`。

## 🗺 路线图

M0 地基（架构 / 设计规范 / 脚手架）✅ → M1 账号体系 → M2 简历制作 → M3 岗位录入 → M4 人岗匹配 → M5 AI 模拟面试 → M6 面试报告 → M7 商业化

## 🤝 参与贡献

见 [CONTRIBUTING.md](CONTRIBUTING.md)。欢迎 Issue 与 PR。

## 💡 关于灵计一动

本项目由 [灵计一动](https://lingji1dong.com) 驱动开发——一款给 AI 开发者的、以灵感驱动开发的进度看板，支持网页与 AI 工具实时同步计划、记录灵感。模考Offer 的每一步开发都用它管理。

## 📄 License

[MIT](LICENSE)
