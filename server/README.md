# Svelte Manage Web Server

Spring Boot 后端服务，按端分为 `admin`（管理后台）和 `web`（小程序/用户网页端）两条入口，面向 `svelte-manage-web` 提供后台 REST API、前端兼容 API、后端元数据驱动、Swagger、Redis 缓存抽象、MySQL/JDBC 数据持久化和 AI 业务生成预览能力。

## 能力清单

- 管理后台路径：`/admin/auth/**`、`/admin/meta/**`、`/admin/ai/**`
- 小程序/用户端路径：`/web/auth/**`、`/web/properties/**`、`/web/applications/**`
- 标准 REST API：`GET /admins`、`POST /admins`、`PUT /admins/{id}`、`DELETE /admins/{id}`
- 前端兼容接口：`GET /admin/list`、`POST /admin/add`、`POST /admin/update`、`POST /admin/delete/{id}`
- 后端元数据驱动：`GET /admin/meta/modules`、`GET /admin/meta/menu`、`GET /admin/meta/permissions`
- AI 业务生成闭环：`POST /admin/ai/modules/generate`、`GET /admin/ai/modules/{taskNo}/preview`、`POST /admin/ai/modules/{taskNo}/apply`、`POST /admin/ai/modules/{taskNo}/rollback`
- 审计日志：关键登录、管理员、AI、上传操作写入结构化 `sys_audit_log`，并镜像到现有 `logs` 资源供后台查看。
- RBAC 权限底座：角色与权限从 `sys_role`、`sys_role_permission` 读取，保留内置角色回退，`GET /admin/meta/permissions` 返回数据库角色元数据。
- 系统设置中心：`/settings` 继续沿用现有 CRUD 接口，数据已迁移到结构化 `sys_system_setting` 表；内置配置允许更新但不允许删除。
- 结构化业务数据：申请、FAQ、字典、日志、设置全部使用独立 MySQL 表，不再使用通用 JSON 资源表。
- 数据持久化：MySQL + Spring JDBC Mapper
- Swagger/OpenAPI：`/swagger-ui.html`、`/v3/api-docs`
- Redis 缓存增强：Redis 可用时承载限流与会话续期，缺省或单机环境会自动降级为本地内存缓存
- 共享文件上传：`POST /file/uploadImg`（管理端与 Web 端共用）

## 后端包结构

更新时间：2026-05-31；执行者：Codex。

后端已统一为 `com.zhoolg.manage` 下的标准扁平分层结构，`admin` 不再作为业务根包，仅保留在 `controller.admin` 中表达管理端入口。

```text
com.zhoolg.manage
├── controller
│   ├── admin              # 管理后台控制器
│   └── web                # 小程序/用户网页端控制器
├── service                # 服务接口与业务服务
│   └── impl               # 服务实现
├── mapper                 # Spring JDBC Mapper
├── infrastructure
│   ├── auth               # 认证基础设施
│   └── crud               # 元数据 CRUD 基础设施
├── entity
│   ├── dto                # 请求/响应 DTO
│   ├── pojo               # 数据对象
│   └── base               # 通用响应、分页、当前用户模型
├── config                 # Spring 与 OpenAPI 配置
├── exception              # 业务异常与全局异常处理
├── cache                  # 缓存抽象
├── constant               # 常量
└── utils                  # 通用工具
```

## 启动

```bash
cd server
mvn spring-boot:run
```

源码/IDE/Maven 开发运行时默认使用 `dev` profile，本地开发配置只放在 `src/main/resources/application-dev.yml`，默认端口：`8080`。如需显式指定，也可以使用 `mvn spring-boot:run -Dspring-boot.run.profiles=dev`。
Arthas 不作为应用内常驻 starter 打包；生产排障建议按需外部 attach，避免 agent 在测试或生产启动时静默常驻。

`application.yml` 不写死数据库、端口、密钥等生产运行配置。打包后的 jar 默认使用 `prod` profile，且不会把 `application-dev.yml` 打进 jar。生产环境使用 jar 同级目录下的外部 `application-prod.yml`，同时提供固定 RSA 私钥；首次初始化管理员密码不再有默认值，需要显式设置 `ADMIN_SEED_PASSWORD` 后启动一次完成播种。

## 配置分层

后端统一使用 MySQL。本地开发 profile 的默认连接在 `application-dev.yml`：

```env
MYSQL_URL=jdbc:mysql://localhost:3306/svelte_manage_web?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
MYSQL_USERNAME=root
MYSQL_PASSWORD=
```

生产部署时，不要把真实配置放进 jar。复制 `server/application-prod.yml.example` 为 jar 同级目录下的 `application-prod.yml`：

```text
deploy/
├── svelte-manage-web-server-0.1.0.jar
├── application-prod.yml
└── secrets/
    └── rsa-private.pem
```

启动生产服务：

```bash
java -jar svelte-manage-web-server-0.1.0.jar
```

打包后的 jar 会自动把默认 profile 切换为 `prod`，并把 jar 所在目录加入外部配置搜索位置，因此同级目录下的 `application-prod.yml` 会被加载。也可以继续通过 `SPRING_PROFILES_ACTIVE=prod` 显式指定。真实 `application-prod.yml` 已加入 `.gitignore`，不要提交到仓库。

本地开发启动时会执行 `schema.sql` 和 `data.sql` 初始化表结构和演示数据；生产模板默认 `spring.sql.init.mode=never`，避免部署时自动重刷数据。

如果需要从空 MySQL 库一次性初始化完整默认功能，可手动导入 `src/main/resources/mysql-init.sql`。该脚本包含表结构、默认账号、角色权限、菜单、系统设置、字典、FAQ、租房申请演示数据和初始化审计记录。

运行本地开发或集成测试前，需要确保 MySQL 服务可连接；否则 Spring 上下文会在数据源初始化阶段失败。

Spring AI 以可插拔模型网关接入，默认关闭外部模型调用。需要模型增强时再通过环境变量开启：

```env
AI_SPRING_AI_ENABLED=true
AI_MODEL_PROVIDER=openai
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-4o-mini
```

Claude/Anthropic 可使用 `AI_MODEL_PROVIDER=anthropic`、`ANTHROPIC_API_KEY` 和 `ANTHROPIC_MODEL`。模型输出只进入生成预览与校验流程，不直接写入生产代码或数据库结构。

## 初始账号

初始账号会随 `data.sql` 创建，但默认没有可登录密码。需要本地初始化时，设置 `ADMIN_SEED_PASSWORD` 后启动一次，系统会为尚未设置密码哈希的账号播种。

| 账号     | 权限     |
| -------- | -------- |
| admin    | `*`      |
| viewer   | 只读     |
| operator | 运营权限 |

## 权限与系统设置

RBAC 使用结构化表承载：

- `sys_role`：角色编码、角色名称、启用状态、是否系统内置。
- `sys_role_permission`：角色到权限字符串的映射，支持 `*` 与 `module:*` 通配符。

认证成功和每次会话校验都会按账号当前角色刷新权限快照。数据库角色未初始化或角色没有权限行时，服务会回落到内置 `super_admin/admin/operator/viewer` 权限，保证本地初始化阶段可用。

系统设置使用 `sys_system_setting`：

- `setting_key` 对应前端字段 `key`
- `setting_name` 对应前端字段 `name`
- `setting_value` 对应前端字段 `value`
- `system_builtin=true` 的内置配置不可删除，避免误删平台启动所需配置

## 前端代理

开发环境已经配置：

```env
VITE_APP_TARGET_URL=http://localhost:8080
```

## 常用入口

| 功能         | URL                                   |
| ------------ | ------------------------------------- |
| Swagger UI   | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs     |
| 健康检查     | http://localhost:8080/actuator/health |

## AI 生成预览与应用示例

本地格式：

```http
POST /admin/ai/modules/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "我要做一个房源报修管理模块，用户可以提交报修，管理员可以派单，维修人员可以完成维修",
  "moduleKey": "repair_order",
  "moduleName": "报修管理",
  "businessType": "crud-workflow"
}
```

OpenAI Chat/Responses 兼容格式：

```http
POST /admin/ai/modules/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "model": "gpt-4.1",
  "messages": [
    {
      "role": "system",
      "content": "你是后台模块生成助手"
    },
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "生成报修管理模块，支持派单、完工和状态流转"
        }
      ]
    }
  ],
  "metadata": {
    "module_key": "repair_order",
    "module_name": "报修管理",
    "business_type": "crud-workflow"
  }
}
```

Claude Messages 兼容格式：

```http
POST /admin/ai/modules/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "model": "claude-3-5-sonnet-latest",
  "system": "你是业务建模助手",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "我要做一个客户回访模块"
        }
      ]
    }
  ],
  "module": {
    "key": "customer_revisit",
    "name": "客户回访"
  }
}
```

返回内容包含：

- 结构化业务 schema
- 符合当前扁平后端结构的 Java 草案：`entity/pojo`、`mapper`、`service`、`service/impl`、`controller/admin`
- MySQL SQL 草案：`generated-sql/create_<module>.sql`
- 前端/后端元数据 JSON 草案

当前 AI 生成默认只做**预览**，不会直接写入生产代码。确认预览后可以把生成模块应用到后端元数据：

```http
POST /admin/ai/modules/{taskNo}/apply
Authorization: Bearer <token>
```

应用前会执行校验报告，检查模块标识、权限、实体字段、MySQL 表名和生成文件路径等结构约束；校验未通过时拒绝应用。

应用后：

- `sys_dynamic_module` 会保存该模块的前端元数据
- `GET /admin/meta/modules` 会返回该动态模块
- 后端会从 `sys_dynamic_module.metadata_json` 构建动态 `ResourceDefinition`，字段白名单、搜索字段、过滤字段、表格列和表单字段以已应用 metadata 为准
- 业务数据不再写入通用 JSON 资源表；动态模块需要按生成 SQL 固化为结构化表后再承载正式 CRUD 数据
- 未应用或未启用的未知资源不会自动创建 CRUD fallback，会返回资源不存在

如需撤销某次已应用的 AI 动态模块，可调用：

```http
POST /admin/ai/modules/{taskNo}/rollback
Authorization: Bearer <token>
```

回滚只会禁用与当前 `taskNo` 绑定的动态模块；如果该模块已经被后续任务覆盖，会拒绝回滚，避免误删新版本。
