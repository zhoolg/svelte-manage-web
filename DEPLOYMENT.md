# Kubernetes 部署指南

本文档详细说明如何将 Svelte 管理后台应用部署到 Kubernetes 集群。

---

## 目录

- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [详细步骤](#详细步骤)
- [配置说明](#配置说明)
- [运维管理](#运维管理)
- [故障排查](#故障排查)
- [最佳实践](#最佳实践)

---

## 前置要求

### 必需工具

- **Docker** >= 20.10
- **Kubernetes** >= 1.24
- **kubectl** >= 1.24
- **pnpm** >= 8.0（用于本地构建）

### 可选工具

- **Kustomize** >= 4.5（用于多环境管理）
- **Helm** >= 3.0（用于包管理）
- **cert-manager**（用于自动 HTTPS 证书）

### 集群要求

- Kubernetes 集群已就绪
- 已安装 Nginx Ingress Controller
- 已配置 StorageClass（如需持久化存储）
- 已配置 Metrics Server（如需 HPA）

---

## 快速开始

### 1. 构建 Docker 镜像

```bash
# 构建镜像
docker build -t your-registry/svelte-admin:latest .

# 推送到镜像仓库
docker push your-registry/svelte-admin:latest
```

### 2. 部署到 Kubernetes

```bash
# 应用所有配置
kubectl apply -f k8s/

# 或使用 Kustomize
kubectl apply -k k8s/
```

### 3. 验证部署

```bash
# 查看 Pod 状态
kubectl get pods -l app=svelte-admin

# 查看服务状态
kubectl get svc svelte-admin

# 查看 Ingress 状态
kubectl get ingress svelte-admin
```

---

## 详细步骤

### 步骤 1: 准备镜像仓库

#### 使用 Docker Hub

```bash
# 登录 Docker Hub
docker login

# 构建并推送
docker build -t username/svelte-admin:v1.0.0 .
docker push username/svelte-admin:v1.0.0
```

#### 使用私有镜像仓库

```bash
# 登录私有仓库
docker login registry.example.com

# 构建并推送
docker build -t registry.example.com/svelte-admin:v1.0.0 .
docker push registry.example.com/svelte-admin:v1.0.0

# 创建镜像拉取密钥
kubectl create secret docker-registry registry-secret \
  --docker-server=registry.example.com \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email@example.com
```

### 步骤 2: 配置环境变量

编辑 `k8s/configmap.yaml`，根据实际环境修改配置：

```yaml
data:
  VITE_APP_TITLE: "你的应用名称"
  VITE_APP_TARGET_URL: "https://your-api-server.com/"
  BACKEND_URL: "https://your-api-server.com/"
```

### 步骤 3: 修改 Deployment 配置

编辑 `k8s/deployment.yaml`：

```yaml
spec:
  replicas: 3  # 根据需求调整副本数
  template:
    spec:
      containers:
      - name: svelte-admin
        image: your-registry/svelte-admin:v1.0.0  # 修改为实际镜像地址
        resources:
          requests:
            cpu: 100m      # 根据实际情况调整
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi
```

### 步骤 4: 配置 Ingress

编辑 `k8s/ingress.yaml`，修改域名：

```yaml
spec:
  tls:
  - hosts:
    - your-domain.com  # 修改为实际域名
    secretName: svelte-admin-tls
  rules:
  - host: your-domain.com  # 修改为实际域名
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: svelte-admin
            port:
              number: 80
```

### 步骤 5: 部署应用

```bash
# 1. 创建 ConfigMap
kubectl apply -f k8s/configmap.yaml

# 2. 创建 Deployment
kubectl apply -f k8s/deployment.yaml

# 3. 创建 Service
kubectl apply -f k8s/service.yaml

# 4. 创建 Ingress
kubectl apply -f k8s/ingress.yaml

# 5. 创建 HPA（可选）
kubectl apply -f k8s/hpa.yaml
```

### 步骤 6: 配置 DNS

将域名解析到 Ingress Controller 的外部 IP：

```bash
# 获取 Ingress Controller 的外部 IP
kubectl get svc -n ingress-nginx

# 配置 DNS A 记录
# your-domain.com -> <EXTERNAL-IP>
```

### 步骤 7: 配置 HTTPS（使用 cert-manager）

```bash
# 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 创建 ClusterIssuer
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

---

## 配置说明

### 资源配置建议

| 环境 | CPU Request | CPU Limit | Memory Request | Memory Limit | 副本数 |
|------|-------------|-----------|----------------|--------------|--------|
| 开发 | 50m         | 200m      | 64Mi           | 256Mi        | 1      |
| 测试 | 100m        | 500m      | 128Mi          | 512Mi        | 2      |
| 生产 | 200m        | 1000m     | 256Mi          | 1Gi          | 3-10   |

### HPA 配置说明

```yaml
minReplicas: 3      # 最小副本数
maxReplicas: 10     # 最大副本数
targetCPUUtilizationPercentage: 70    # CPU 目标使用率
targetMemoryUtilizationPercentage: 80 # 内存目标使用率
```

### Ingress 注解说明

```yaml
annotations:
  # SSL 重定向
  nginx.ingress.kubernetes.io/ssl-redirect: "true"

  # 请求体大小限制
  nginx.ingress.kubernetes.io/proxy-body-size: "10m"

  # 超时配置
  nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
  nginx.ingress.kubernetes.io/proxy-read-timeout: "60"

  # 速率限制
  nginx.ingress.kubernetes.io/limit-rps: "100"

  # CORS 配置
  nginx.ingress.kubernetes.io/enable-cors: "true"
```

---

## 运维管理

### 查看应用状态

```bash
# 查看 Pod 状态
kubectl get pods -l app=svelte-admin

# 查看 Pod 详情
kubectl describe pod <pod-name>

# 查看 Pod 日志
kubectl logs -f <pod-name>

# 查看最近的事件
kubectl get events --sort-by=.metadata.creationTimestamp
```

### 扩缩容操作

```bash
# 手动扩容
kubectl scale deployment svelte-admin --replicas=5

# 查看 HPA 状态
kubectl get hpa svelte-admin

# 查看 HPA 详情
kubectl describe hpa svelte-admin
```

### 滚动更新

```bash
# 更新镜像
kubectl set image deployment/svelte-admin \
  svelte-admin=your-registry/svelte-admin:v1.0.1

# 查看更新状态
kubectl rollout status deployment/svelte-admin

# 查看更新历史
kubectl rollout history deployment/svelte-admin

# 回滚到上一个版本
kubectl rollout undo deployment/svelte-admin

# 回滚到指定版本
kubectl rollout undo deployment/svelte-admin --to-revision=2
```

### 配置更新

```bash
# 更新 ConfigMap
kubectl edit configmap svelte-admin-config

# 重启 Pod 使配置生效
kubectl rollout restart deployment/svelte-admin
```

### 资源监控

```bash
# 查看资源使用情况
kubectl top pods -l app=svelte-admin

# 查看节点资源使用
kubectl top nodes

# 查看 HPA 指标
kubectl get hpa svelte-admin --watch
```

---

## 故障排查

### Pod 无法启动

```bash
# 查看 Pod 状态
kubectl get pods -l app=svelte-admin

# 查看 Pod 事件
kubectl describe pod <pod-name>

# 查看容器日志
kubectl logs <pod-name>

# 查看上一次容器日志（如果容器重启了）
kubectl logs <pod-name> --previous
```

**常见问题：**

1. **ImagePullBackOff**
   - 检查镜像地址是否正确
   - 检查镜像拉取密钥是否配置
   - 检查镜像仓库权限

2. **CrashLoopBackOff**
   - 查看容器日志
   - 检查健康检查配置
   - 检查资源限制是否合理

3. **Pending**
   - 检查节点资源是否充足
   - 检查 PVC 是否正常绑定
   - 检查节点亲和性配置

### 服务无法访问

```bash
# 测试 Service 连通性
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
wget -O- http://svelte-admin

# 查看 Service 端点
kubectl get endpoints svelte-admin

# 查看 Ingress 状态
kubectl describe ingress svelte-admin
```

**常见问题：**

1. **Service 无端点**
   - 检查 Pod 是否就绪
   - 检查 Service selector 是否匹配

2. **Ingress 无法访问**
   - 检查 DNS 解析
   - 检查 Ingress Controller 状态
   - 检查证书是否有效

### 性能问题

```bash
# 查看资源使用
kubectl top pods -l app=svelte-admin

# 查看 HPA 状态
kubectl get hpa svelte-admin

# 查看 Pod 事件
kubectl get events --field-selector involvedObject.name=<pod-name>
```

**优化建议：**

1. 调整资源限制
2. 增加副本数
3. 启用 HPA
4. 优化镜像大小
5. 使用 CDN 加速静态资源

---

## 最佳实践

### 1. 镜像管理

- 使用语义化版本标签（如 v1.0.0）
- 避免使用 `latest` 标签
- 定期清理旧镜像
- 使用多阶段构建减小镜像体积

### 2. 资源管理

- 始终设置资源请求和限制
- 根据实际使用情况调整资源配置
- 使用 HPA 实现自动扩缩容
- 配置 Pod 反亲和性提高可用性

### 3. 安全配置

- 使用非 root 用户运行容器
- 启用 SecurityContext
- 定期更新基础镜像
- 使用 Secret 管理敏感信息
- 配置网络策略限制流量

### 4. 监控告警

- 配置健康检查（liveness、readiness、startup）
- 集成监控系统（Prometheus、Grafana）
- 配置日志收集（ELK、Loki）
- 设置告警规则

### 5. 备份恢复

- 定期备份配置文件
- 使用版本控制管理配置
- 测试恢复流程
- 记录变更历史

### 6. 多环境管理

使用 Kustomize 管理多环境：

```bash
# 开发环境
kubectl apply -k k8s/overlays/dev

# 生产环境
kubectl apply -k k8s/overlays/prod
```

### 7. CI/CD 集成

```yaml
# GitLab CI 示例
deploy:
  stage: deploy
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_TAG .
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_TAG
    - kubectl set image deployment/svelte-admin svelte-admin=$CI_REGISTRY_IMAGE:$CI_COMMIT_TAG
  only:
    - tags
```

---

## 清理资源

```bash
# 删除所有资源
kubectl delete -f k8s/

# 或使用 Kustomize
kubectl delete -k k8s/

# 删除命名空间（如果使用了独立命名空间）
kubectl delete namespace svelte-admin
```

---

## 参考资源

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [cert-manager 文档](https://cert-manager.io/docs/)
- [Kustomize 文档](https://kustomize.io/)

---

## 技术支持

如有问题，请提交 Issue 或联系运维团队。
