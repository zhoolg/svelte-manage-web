# Svelte Manage Web

Svelte Manage Web 是一个面向后台管理与业务生成场景的全栈管理平台模板，前端使用 Svelte 5、TypeScript、Vite 和 Tailwind CSS，后端使用 Spring Boot、Spring JDBC、MySQL 和 Redis。

## 功能特性

- 管理后台：登录、管理员、菜单、权限、字典、设置、日志、系统状态。
- 元数据驱动：后端提供模块、菜单、权限和 CRUD 元数据，前端按配置渲染管理界面。
- AI 业务生成：提供模块生成、预览、应用、回滚和运行时冒烟验证接口。
- Web/小程序端接口：提供用户端登录、资料、申请等业务入口。
- 文件上传：管理端与 Web 端共用图片上传接口。
- 私有扩展库：`private-libs/` 存放混淆后的授权与 AI 扩展运行库，源码不包含在公开仓库中。

## 目录结构

```text
svelte-manage-web
├── web/                  # Svelte 管理后台前端
├── server/               # Spring Boot 后端服务
├── mobile/               # 移动端/小程序相关工程
├── private-libs/         # 可发布的混淆私有运行库
├── .github/              # Issue 与 PR 模板
├── CONTRIBUTING.md       # 贡献指南
├── SECURITY.md           # 安全说明
├── LICENSE               # 许可证
└── NOTICE                # 第三方声明
```

## 环境要求

- Node.js 22 或更高版本
- npm
- JDK 21
- Maven 3.9 或更高版本
- MySQL 8
- Redis 7（可选；不可用时后端会降级为本地内存缓存）

## 本地开发

安装前端依赖：

```bash
cd web
npm install
```

启动前端开发服务：

```bash
npm run dev
```

启动后端开发服务：

```bash
cd web
npm run server:dev
```

后端默认开发端口为 `8080`，前端开发端口由 `web/.env.development` 中的 `VITE_PORT` 控制。

## 构建与检查

前端类型检查与构建：

```bash
cd web
npm run check
npm run build
```

后端测试与打包：

```bash
cd web
npm run server:check
npm run server:build
```

## 生产配置

生产环境不要提交真实配置、密钥、证书或授权文件。复制 `server/application-prod.yml.example` 为部署目录中的 `application-prod.yml`，并通过环境变量或外部文件提供数据库、Redis、RSA 私钥和 AI 凭据配置。

示例部署结构：

```text
deploy/
├── svelte-manage-web-server-0.1.0.jar
├── application-prod.yml
└── secrets/
    └── rsa-private.pem
```

启动服务：

```bash
java -jar svelte-manage-web-server-0.1.0.jar
```

## 发布产物

`private-libs/` 中的 JAR 是公开仓库可携带的混淆运行库，用于授权校验和 AI 扩展能力。不要提交未混淆私有源码、私钥、授权文件、证书、生产配置或构建缓存。

`release/`、`web/dist/`、`server/target/` 等构建产物目录已被 `.gitignore` 忽略。

## 许可证

本项目许可证见 `LICENSE`，第三方声明见 `NOTICE`。
