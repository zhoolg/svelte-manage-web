# Kubernetes 部署方案总结

## 📋 概述

本文档总结了为 Svelte 管理后台项目创建的完整 Kubernetes 部署方案。

---

## 🎯 方案特点

### 1. 生产级就绪

- ✅ 多阶段 Docker 构建，镜像体积优化
- ✅ 完整的健康检查配置（Liveness、Readiness、Startup）
- ✅ 资源请求和限制配置
- ✅ 安全上下文配置（非 root 用户运行）
- ✅ 滚动更新策略（零停机部署）

### 2. 高可用性

- ✅ 多副本部署（默认 3 个）
- ✅ Pod 反亲和性配置（分散在不同节点）
- ✅ HPA 自动扩缩容（3-10 副本）
- ✅ 健康检查自动重启故障 Pod

### 3. 易于管理

- ✅ ConfigMap 管理环境配置
- ✅ Kustomize 支持多环境部署
- ✅ Makefile 简化常用操作
- ✅ 完整的部署文档和故障排查指南

### 4. 安全性

- ✅ 非 root 用户运行容器
- ✅ 只读根文件系统（可选）
- ✅ 最小权限原则
- ✅ HTTPS 支持（通过 Ingress + cert-manager）
- ✅ 安全头配置（Nginx）

---

## 📁 文件结构

```
svelte-manage-web/
├── Dockerfile                 # Docker 多阶段构建配置
├── .dockerignore             # Docker 忽略文件
├── nginx.conf                # Nginx 配置文件
├── docker-compose.yml        # Docker Compose 配置
├── Makefile                  # 常用命令简化
├── DEPLOYMENT.md             # 详细部署文档
├── K8S-SUMMARY.md           # 本文档
└── k8s/                      # Kubernetes 配置目录
    ├── README.md             # K8s 配置说明
    ├── configmap.yaml        # 环境配置
    ├── deployment.yaml       # Deployment 配置
    ├── service.yaml          # Service 配置
    ├── ingress.yaml          # Ingress 配置
    ├── hpa.yaml              # 自动扩缩容配置
    └── kustomization.yaml    # Kustomize 配置
```

---

##  部署流程

### 阶段 1: 准备镜像

```bash
# 1. 构建 Docker 镜像
docker build -t your-registry/svelte-admin:v1.0.0 .

# 2. 推送到镜像仓库
docker push your-registry/svelte-admin:v1.0.0
```

### 阶段 2: 配置修改

```bash
# 1. 修改镜像地址
# 编辑 k8s/deployment.yaml，修改 image 字段

# 2. 修改环境配置
# 编辑 k8s/configmap.yaml，修改环境变量

# 3. 修改域名
# 编辑 k8s/ingress.yaml，修改 host 字段
```

### 阶段 3: 部署到 K8s

```bash
# 方式 1: 直接部署
kubectl apply -f k8s/

# 方式 2: 使用 Makefile
make deploy

# 方式 3: 使用 Kustomize
kubectl apply -k k8s/
```

### 阶段 4: 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -l app=svelte-admin

# 查看服务
kubectl get svc svelte-admin

# 查看 Ingress
kubectl get ingress svelte-admin

# 查看日志
kubectl logs -f -l app=svelte-admin
```

---

## 🔧 核心配置说明

### 1. Deployment 配置

**关键特性：**
- 副本数：3（生产环境）
- 滚动更新：maxSurge=1, maxUnavailable=0
- 资源限制：CPU 100m-500m, Memory 128Mi-512Mi
- 健康检查：完整的三种探针配置
- 安全上下文：非 root 用户（UID 101）

**配置文件：** `k8s/deployment.yaml`

### 2. Service 配置

**关键特性：**
- 类型：ClusterIP（内部访问）
- 会话亲和性：ClientIP（3小时）
- 端口：80

**配置文件：** `k8s/service.yaml`

### 3. Ingress 配置

**关键特性：**
- HTTPS 自动重定向
- CORS 支持
- 请求体大小限制：10MB
- 超时配置：60秒
- 速率限制：100 RPS
- cert-manager 自动证书

**配置文件：** `k8s/ingress.yaml`

### 4. HPA 配置

**关键特性：**
- 副本范围：3-10
- CPU 目标：70%
- 内存目标：80%
- 扩容策略：快速扩容（0秒稳定窗口）
- 缩容策略：缓慢缩容（5分钟稳定窗口）

**配置文件：** `k8s/hpa.yaml`

### 5. ConfigMap 配置

**关键特性：**
- 应用配置（标题、版本等）
- API 配置（后端地址）
- 功能开关（Mock、开发登录）
- 多环境支持（dev、prod）

**配置文件：** `k8s/configmap.yaml`

---

## 📊 资源配置建议

### 开发环境

```yaml
replicas: 1
resources:
  requests:
    cpu: 50m
    memory: 64Mi
  limits:
    cpu: 200m
    memory: 256Mi
```

**适用场景：** 本地开发、功能测试

### 测试环境

```yaml
replicas: 2
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

**适用场景：** 集成测试、压力测试

### 生产环境

```yaml
replicas: 3-10 (HPA)
resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

**适用场景：** 生产环境、高并发场景

---

## 🔒 安全配置

### 1. 容器安全

```yaml
securityContext:
  runAsNonRoot: true          # 非 root 用户
  runAsUser: 101              # nginx 用户
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: false
  capabilities:
    drop:
    - ALL
    add:
    - NET_BIND_SERVICE
```

### 2. Nginx 安全头

```nginx
# 隐藏版本号
server_tokens off;

# 安全头
add_header X-Frame-Options "SAMEORIGIN";
add_header X-Content-Type-Options "nosniff";
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "no-referrer-when-downgrade";
```

### 3. HTTPS 配置

使用 cert-manager 自动签发 Let's Encrypt 证书：

```yaml
annotations:
  cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

---

## 📈 监控和告警

### 1. 健康检查

- **Liveness Probe**: 检测容器是否存活
- **Readiness Probe**: 检测容器是否就绪
- **Startup Probe**: 检测容器是否启动完成

### 2. 资源监控

```bash
# 查看 Pod 资源使用
kubectl top pods -l app=svelte-admin

# 查看 HPA 状态
kubectl get hpa svelte-admin-hpa
```

### 3. 日志收集

```bash
# 实时查看日志
kubectl logs -f -l app=svelte-admin

# 查看最近 100 行日志
kubectl logs -l app=svelte-admin --tail=100
```

---

## 🛠️ 运维操作

### 1. 扩缩容

```bash
# 手动扩容
kubectl scale deployment svelte-admin --replicas=5

# 或使用 Makefile
make scale REPLICAS=5
```

### 2. 滚动更新

```bash
# 更新镜像
kubectl set image deployment/svelte-admin \
  svelte-admin=your-registry/svelte-admin:v1.0.1

# 或使用 Makefile
make update IMAGE_TAG=v1.0.1
```

### 3. 回滚

```bash
# 回滚到上一版本
kubectl rollout undo deployment/svelte-admin

# 或使用 Makefile
make rollback
```

### 4. 重启

```bash
# 重启所有 Pod
kubectl rollout restart deployment/svelte-admin

# 或使用 Makefile
make restart
```

---

## 🐛 故障排查

### 常见问题

#### 1. Pod 无法启动

**症状：** Pod 状态为 `CrashLoopBackOff` 或 `ImagePullBackOff`

**排查步骤：**
```bash
# 查看 Pod 详情
kubectl describe pod <pod-name>

# 查看日志
kubectl logs <pod-name>

# 查看上一次日志
kubectl logs <pod-name> --previous
```

#### 2. 服务无法访问

**症状：** 无法通过 Ingress 访问应用

**排查步骤：**
```bash
# 检查 Service 端点
kubectl get endpoints svelte-admin

# 检查 Ingress 状态
kubectl describe ingress svelte-admin

# 测试 Service 连通性
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
wget -O- http://svelte-admin
```

#### 3. HPA 不工作

**症状：** HPA 无法获取指标

**排查步骤：**
```bash
# 检查 Metrics Server
kubectl top nodes

# 查看 HPA 详情
kubectl describe hpa svelte-admin-hpa
```

---

## 📚 相关文档

- [DEPLOYMENT.md](./DEPLOYMENT.md) - 详细部署指南
- [k8s/README.md](./k8s/README.md) - K8s 配置说明
- [Dockerfile](./Dockerfile) - Docker 构建配置
- [Makefile](./Makefile) - 常用命令

---

## 🎓 最佳实践

### 1. 镜像管理

- ✅ 使用语义化版本标签（v1.0.0）
- ✅ 避免使用 `latest` 标签
- ✅ 定期更新基础镜像
- ✅ 使用多阶段构建减小体积

### 2. 资源管理

- ✅ 始终设置资源请求和限制
- ✅ 根据实际使用调整配置
- ✅ 使用 HPA 实现自动扩缩容
- ✅ 配置 Pod 反亲和性

### 3. 配置管理

- ✅ 使用 ConfigMap 管理配置
- ✅ 使用 Secret 管理敏感信息
- ✅ 使用 Kustomize 管理多环境
- ✅ 版本控制所有配置文件

### 4. 监控告警

- ✅ 配置完整的健康检查
- ✅ 集成监控系统（Prometheus）
- ✅ 配置日志收集（ELK/Loki）
- ✅ 设置告警规则

### 5. 安全加固

- ✅ 使用非 root 用户
- ✅ 启用 SecurityContext
- ✅ 配置网络策略
- ✅ 定期安全扫描

---

##  快速命令参考

```bash
# 部署
make deploy                    # 部署到 K8s
make status                    # 查看状态
make logs                      # 查看日志

# 更新
make update IMAGE_TAG=v1.0.1   # 更新镜像
make rollback                  # 回滚
make restart                   # 重启

# 扩缩容
make scale REPLICAS=5          # 扩容到 5 个副本

# 调试
make exec                      # 进入容器
make events                    # 查看事件
make top                       # 查看资源使用

# 清理
make clean                     # 删除所有资源
```

---

## 📞 技术支持

如有问题，请：
1. 查看 [DEPLOYMENT.md](./DEPLOYMENT.md) 详细文档
2. 查看 [k8s/README.md](./k8s/README.md) 配置说明
3. 提交 GitHub Issue
4. 联系运维团队

---

## 📝 更新日志

### v1.0.0 (2025-11-30)

- ✅ 初始版本
- ✅ 完整的 K8s 部署配置
- ✅ Docker 多阶段构建
- ✅ HPA 自动扩缩容
- ✅ Ingress HTTPS 支持
- ✅ 完整的部署文档

