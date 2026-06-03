# Svelte Manage Web

Svelte Manage Web 是一个前后端分离的管理后台项目，仓库按 `web/` 和 `server/` 两个目录组织：

- `web/`：Svelte 5 + TypeScript + Vite + Tailwind CSS 管理后台。
- `server/`：Spring Boot + MySQL + Redis 后端服务，提供认证、RBAC、动态 CRUD、菜单元数据、AI 模块生成和系统状态接口。

## 目录结构

```text
svelte-manage-web/
├── web/                       # 前端应用
│   ├── src/                   # Svelte 源码
│   ├── public/                # 静态资源
│   ├── package.json           # 前端脚本与依赖
│   └── README.md              # 前端详细说明
├── server/                    # 后端服务
│   ├── src/main/java/         # Spring Boot 源码
│   ├── src/main/resources/    # 配置、schema、初始化 SQL
│   ├── pom.xml                # Maven 配置
│   └── README.md              # 后端详细说明
├── README.md                  # 仓库总览
├── SECURITY.md                # 安全说明
├── CONTRIBUTING.md            # 贡献指南
├── CODE_OF_CONDUCT.md         # 社区行为准则
├── LICENSE                    # GPL-3.0
└── NOTICE
```

## 技术栈

- 前端：Svelte 5、TypeScript Native Preview、Vite 7、Tailwind CSS 4、Bits UI、Lucide。
- 后端：Spring Boot 3.5、Java 21、Spring Security、Spring JDBC、MySQL、Redis、Spring AI。
- 数据：结构化 MySQL 表承载用户、角色、权限、菜单、系统设置、动态模块与业务数据。

## 本地开发

先启动后端：

```bash
cd server
mvn spring-boot:run
```

源码/IDE/Maven 开发运行时默认使用 `dev` profile，读取 `server/src/main/resources/application-dev.yml`。后端默认端口是 `8080`。

再启动前端：

```bash
cd web
npm install
npm run dev
```

前端默认运行在 `http://localhost:7052`，开发代理会把 `/api` 转发到 `http://localhost:8080`。

## 常用命令

```bash
# 前端检查
cd web
npm run check

# 前端构建
npm run build

# 后端测试
cd ../server
mvn test

# 后端打包
mvn -DskipTests package
```

也可以在 `web/` 下通过脚本调用后端：

```bash
npm run server:dev
npm run server:check
npm run server:build
```

## 生产部署

后端打包后不会把 `application-dev.yml` 放进 jar。可执行 jar 默认使用 `prod` profile，并自动加载 jar 同级目录下的 `application-prod.yml`。

```text
deploy/
├── svelte-manage-web-server-0.1.0.jar
├── application-prod.yml
└── secrets/
    └── rsa-private.pem
```

生产配置模板见 `server/application-prod.yml.example`，真实 `application-prod.yml` 已加入 `.gitignore`，不要提交到 GitHub。

前端生产构建在 `web/dist/`，通常由 Nginx 或同类静态服务托管，并把 `/api` 反向代理到后端。

## 文档

- 前端详细说明：[web/README.md](web/README.md)
- 后端详细说明：[server/README.md](server/README.md)
- 安全说明：[SECURITY.md](SECURITY.md)

## License

GPL-3.0 License
