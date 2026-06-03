# server 设计文档

日期：2026-05-31
执行者：Codex

## 设计概述

`server` 是 `svelte-manage-web` 的 Spring Boot 后端服务，负责提供管理后台接口、用户端示例接口、元数据驱动 CRUD、Swagger 文档、Redis 缓存抽象、MySQL/JDBC 数据持久化和 AI 业务生成预览闭环。

当前代码包采用 `com.zhoolg.manage` 下的标准扁平分层结构，避免按历史业务域建立 `admin/auth/crud/metadata/user/ai` 等根包。

## 包结构

```text
com.zhoolg.manage
├── controller
│   ├── admin              # 管理后台入口
│   └── web                # 用户端 / 小程序入口
├── service                # 业务服务接口与领域服务
│   └── impl               # 服务实现
├── mapper                 # Spring JDBC 数据访问层
├── infrastructure
│   ├── auth               # 认证支撑能力
│   └── crud               # 元数据 CRUD 基础设施
├── entity
│   ├── base               # 通用响应、分页、当前用户
│   ├── dto                # 请求 / 响应 DTO
│   ├── enums              # 枚举
│   └── pojo               # 数据对象
├── config                 # Spring、Web、OpenAPI、加密配置
├── exception              # 业务异常与全局异常处理
├── cache                  # 缓存抽象
├── constant               # 常量
├── interceptor            # 拦截器
└── utils                  # 通用工具
```

## 核心组件

- `controller.admin.AuthController`：管理后台登录、登出和当前用户接口。
- `controller.admin.AdminUserController`：管理员账号管理接口。
- `controller.admin.ResourceRestController`：标准 REST 资源接口。
- `controller.admin.MetadataController`：菜单、模块、权限元数据接口。
- `controller.FileController`：管理端与 Web 端共用文件上传接口。
- `controller.web.WebAuthController`：用户端登录示例接口。
- `controller.web.WebPropertyController`：用户端房源列表与详情示例接口。
- `controller.web.WebApplicationController`：用户端申请提交示例接口。
- `service.impl.CrudServiceImpl`：通用 CRUD 业务编排。
- `service.AiGenerationService`：生成结构化 schema、代码草案、SQL 草案和元数据草案。
- `service.DynamicModuleService`：动态模块元数据读取与应用。
- `service.UserDirectoryService`：管理员账号目录服务。
- `infrastructure.crud.ResourceRegistry`：内置资源定义注册表。
- `infrastructure.crud.DatabaseResourceRepository`：基于 Spring JDBC 的资源数据仓库。
- `mapper.*Mapper`：统一数据访问层。

## 设计决策

| 日期       | 决策                                 | 理由                                             | 影响                                                                                     |
| ---------- | ------------------------------------ | ------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| 2026-05-31 | 采用扁平分层包结构                   | 与项目参考架构保持一致，降低迁移后的包路径复杂度 | 业务入口收敛到 `controller.admin` / `controller.web`                                     |
| 2026-05-31 | Mapper 统一放入 `mapper`             | 避免按业务域散落在多个子包中                     | 导入路径稳定，便于替换持久化实现                                                         |
| 2026-05-31 | CRUD 能力放入 `infrastructure.crud`  | 将通用资源能力与业务服务区分                     | 服务层保持业务编排职责                                                                   |
| 2026-05-31 | 认证支撑放入 `infrastructure.auth`   | 将验证码、加密、初始化等支撑能力与业务接口分离   | `service.impl.AuthServiceImpl` 只负责认证流程编排                                        |
| 2026-05-31 | AI 生成只应用元数据                  | 当前阶段聚焦模块预览与动态菜单应用               | 不直接写入 Java/SQL 源码                                                                 |
| 2026-05-31 | 数据库统一使用 MySQL                 | 与项目运行环境保持一致，移除内存库双轨配置       | 本地编译不依赖数据库，启动和集成测试需要可用 MySQL                                       |
| 2026-05-31 | AI 生成请求兼容 OpenAI / Claude 格式 | 允许外部编排器直接复用主流模型 API 请求体        | `AiGenerateRequest` 从本地字段、`messages`、`input`、`metadata` 中提取业务描述和模块信息 |

## 技术选型

- **Java 21 + Spring Boot 3.5**：后端应用基础。
- **Spring JDBC Mapper**：保持显式 SQL 和 Mapper 分层，减少额外框架迁移成本。
- **MySQL**：统一运行时数据库，使用 `schema.sql` 与 `data.sql` 初始化结构和演示数据。
- **Springdoc OpenAPI**：提供 Swagger UI 与 OpenAPI JSON。
- **Spring Data Redis**：作为可选缓存能力，默认不强制依赖真实 Redis 服务。

## 当前限制

- 申请、FAQ、字典、日志、设置均已迁移为结构化 MySQL 表，业务数据不再落通用资源仓库。
- 动态模块元数据当前通过 `ResourceRegistry` 与 `sys_dynamic_module` 组合提供；动态模块需要按生成 SQL 固化为结构化表后再承载正式 CRUD 数据。
- AI 生成流程当前只生成预览和元数据应用结果，不生成可直接提交的源码变更。

## 变更历史

### 2026-05-31 - 扁平包结构收敛

**变更内容**：删除一次性迁移脚本和旧重构计划，设计文档同步为当前 `controller / service / mapper / entity / infrastructure` 扁平分层结构。

**变更理由**：避免旧纵向业务根包和历史迁移步骤干扰后续检索、维护与架构判断。
