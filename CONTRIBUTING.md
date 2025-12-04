# 贡献指南 (Contributing Guide)

感谢您对 Svelte 管理后台框架的关注！我们欢迎任何形式的贡献。

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [许可证协议](#许可证协议)

## 行为准则

参与本项目即表示您同意遵守我们的行为准则。我们致力于为所有人提供友好、安全和欢迎的环境。

## 如何贡献

### 报告问题 (Bug Reports)

在提交问题之前，请确保：

1. 搜索现有的 [Issues](https://github.com/zhoolg/svelte-manage-web/issues) 确保问题未被报告
2. 使用最新版本测试问题是否依然存在
3. 提供详细的复现步骤和环境信息

**问题模板：**

```markdown
**问题描述**
简要描述问题

**复现步骤**
1. 打开 '...'
2. 点击 '...'
3. 滚动到 '...'
4. 看到错误

**期望行为**
描述您期望发生什么

**实际行为**
描述实际发生了什么

**环境信息**
- 操作系统: [例如 Windows 11]
- 浏览器: [例如 Chrome 120]
- Node 版本: [例如 20.10.0]
- 项目版本: [例如 v1.0.0]

**截图**
如果适用，添加截图以帮助解释问题

**其他信息**
添加关于问题的任何其他信息
```

### 功能请求 (Feature Requests)

我们欢迎新功能建议！请提供：

1. 功能的详细描述
2. 使用场景和价值
3. 可能的实现方案（如果有）
4. 是否愿意自己实现

### 提交代码 (Pull Requests)

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 开发流程

### 1. 环境准备

```bash
# 克隆仓库
git clone https://github.com/zhoolg/svelte-manage-web.git
cd svelte-manage-web

# 安装依赖（推荐使用 pnpm）
pnpm install

# 启动开发服务器
pnpm dev
```

### 2. 创建分支

```bash
# 功能开发
git checkout -b feature/your-feature-name

# Bug 修复
git checkout -b fix/your-bug-fix

# 文档更新
git checkout -b docs/your-doc-update
```

### 3. 开发

- 确保代码遵循项目的代码规范
- 编写清晰的注释
- 添加必要的测试
- 更新相关文档

### 4. 提交前检查

```bash
# 类型检查
pnpm check

# 构建测试
pnpm build
```

### 5. 提交 Pull Request

- 提供清晰的 PR 标题和描述
- 关联相关的 Issue
- 确保 CI 检查通过
- 等待代码审查

## 代码规范

### TypeScript/JavaScript

- 使用 TypeScript 编写代码
- 遵循 ESLint 配置
- 使用有意义的变量和函数名
- 添加适当的类型注解

```typescript
// ✅ 好的示例
interface User {
  id: number;
  name: string;
  email: string;
}

function getUserById(id: number): User | null {
  // 实现...
}

// ❌ 避免
function get(x) {
  // 实现...
}
```

### Svelte 组件

- 组件文件使用 PascalCase 命名
- Props 使用描述性名称
- 导出必要的类型定义

```svelte
<!-- ✅ 好的示例 -->
<script lang="ts">
  interface Props {
    title: string;
    items: Item[];
    onSelect?: (item: Item) => void;
  }

  let { title, items, onSelect }: Props = $props();
</script>

<div class="component">
  <h2>{title}</h2>
  <!-- 组件内容 -->
</div>
```

### CSS/样式

- 优先使用 Tailwind CSS 工具类
- 自定义样式使用有意义的类名
- 避免使用 `!important`

## 提交规范

我们使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构（既不是新功能也不是 Bug 修复）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例

```bash
# 新功能
git commit -m "feat(crud): 添加批量导出功能"

# Bug 修复
git commit -m "fix(table): 修复分页器在数据为空时的显示问题"

# 文档更新
git commit -m "docs(readme): 更新安装说明"

# 重构
git commit -m "refactor(api): 重构 API 请求层"
```

## 许可证协议

### Apache License 2.0

本项目采用 Apache License 2.0 开源协议。向本项目贡献代码即表示您同意：

1. **授予许可**
   - 您授予本项目永久的、全球性的、非独占的、免费的、不可撤销的版权许可
   - 您授予本项目永久的、全球性的、非独占的、免费的、不可撤销的专利许可

2. **贡献声明**
   - 您声明您有权授予上述许可
   - 您的贡献是您的原创作品，或您有权提交该作品
   - 您的贡献不侵犯任何第三方的知识产权

3. **代码归属**
   - 提交的代码将成为项目的一部分
   - 您的贡献将在 Apache 2.0 协议下发布
   - 您将在 NOTICE 文件或代码注释中被标注为贡献者

### 贡献者许可协议 (CLA)

对于重大贡献，我们可能会要求您签署贡献者许可协议。这确保：

- 项目可以自由使用您的贡献
- 您保留您作品的版权
- 保护项目免受法律风险

详见 [CLA.md](./CLA.md)（如适用）

### 版权声明

在新文件中添加以下版权声明：

```typescript
/*
 * Copyright 2025 zhoolg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

对现有文件的修改，在文件头部添加：

```typescript
/*
 * Modifications Copyright 2025 Your Name
 *
 * Licensed under the Apache License, Version 2.0
 */
```

## 代码审查流程

1. **自动检查**
   - CI 自动运行类型检查和构建
   - 确保所有检查通过

2. **人工审查**
   - 维护者会审查您的代码
   - 可能会提出修改建议
   - 请及时响应反馈

3. **合并**
   - 审查通过后，维护者会合并 PR
   - 您的贡献将出现在下一个版本中

## 社区支持

- **GitHub Issues**: 报告问题和功能请求
- **GitHub Discussions**: 技术讨论和问答
- **Email**: 私密问题可发送至项目维护者邮箱

## 致谢

感谢所有贡献者的辛勤付出！您的贡献让这个项目变得更好。

---

再次感谢您的贡献！如有任何问题，请随时联系我们。
