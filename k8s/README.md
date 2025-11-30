# Kubernetes 配置文件说明

本目录包含 Svelte 管理后台应用的 Kubernetes 部署配置文件。

## 文件列表

| 文件 | 说明 |
|------|------|
| `configmap.yaml` | 应用配置（环境变量） |
| `deployment.yaml` | Deployment 配置（Pod 模板、副本数、资源限制等） |
| `service.yaml` | Service 配置（服务暴露） |
| `ingress.yaml` | Ingress 配置（外部访问、HTTPS） |
| `hpa.yaml` | HorizontalPodAutoscaler 配置（自动扩缩容） |
| `kustomization.yaml` | Kustomize 配置（多环境管理） |

## 快速部署

### 方式 1: 直接部署

```bash
# 部署所有资源
kubectl apply -f .

# 查看部署状态
kubectl get all -l app=svelte-admin
```

### 方式 2: 使用 Kustomize

```bash
# 部署
kubectl apply -k .

# 删除
kubectl delete -k .
```

### 方式 3: 逐个部署

```bash
# 1. ConfigMap
kubectl apply -f configmap.yaml

# 2. Deployment
kubectl apply -f deployment.yaml

# 3. Service
kubectl apply -f service.yaml

# 4. Ingress
kubectl apply -f ingress.yaml

# 5. HPA（可选）
kubectl apply -f hpa.yaml
```

## 配置修改指南

### 1. 修改镜像地址

编辑 `deployment.yaml`:

```yaml
spec:
  template:
    spec:
      containers:
      - name: svelte-admin
        image: your-registry/svelte-admin:v1.0.0  # 修改这里
```

### 2. 修改副本数

编辑 `deployment.yaml`:

```yaml
spec:
  replicas: 3  # 修改副本数
```

### 3. 修改资源限制

编辑 `deployment.yaml`:

```yaml
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

### 4. 修改域名

编辑 `ingress.yaml`:

```yaml
spec:
  tls:
  - hosts:
    - your-domain.com  # 修改域名
  rules:
  - host: your-domain.com  # 修改域名
```

### 5. 修改环境变量

编辑 `configmap.yaml`:

```yaml
data:
  VITE_APP_TITLE: "你的应用名称"
  VITE_APP_TARGET_URL: "https://your-api.com/"
```

## 环境配置

### 开发环境

使用 `svelte-admin-config-dev` ConfigMap:

```bash
# 修改 deployment.yaml 中的 configMapRef
envFrom:
- configMapRef:
    name: svelte-admin-config-dev
```

### 生产环境

使用 `svelte-admin-config-prod` ConfigMap:

```bash
# 修改 deployment.yaml 中的 configMapRef
envFrom:
- configMapRef:
    name: svelte-admin-config-prod
```

## 常用命令

### 查看资源

```bash
# 查看所有资源
kubectl get all -l app=svelte-admin

# 查看 Pods
kubectl get pods -l app=svelte-admin

# 查看 Service
kubectl get svc svelte-admin

# 查看 Ingress
kubectl get ingress svelte-admin

# 查看 HPA
kubectl get hpa svelte-admin-hpa
```

### 查看日志

```bash
# 查看所有 Pod 日志
kubectl logs -l app=svelte-admin -f

# 查看特定 Pod 日志
kubectl logs <pod-name> -f

# 查看上一次容器日志
kubectl logs <pod-name> --previous
```

### 更新部署

```bash
# 更新镜像
kubectl set image deployment/svelte-admin \
  svelte-admin=your-registry/svelte-admin:v1.0.1

# 查看更新状态
kubectl rollout status deployment/svelte-admin

# 回滚
kubectl rollout undo deployment/svelte-admin
```

### 扩缩容

```bash
# 手动扩容
kubectl scale deployment svelte-admin --replicas=5

# 查看 HPA 状态
kubectl get hpa svelte-admin-hpa
```

### 调试

```bash
# 进入容器
kubectl exec -it <pod-name> -- sh

# 查看详细信息
kubectl describe pod <pod-name>

# 查看事件
kubectl get events --sort-by=.metadata.creationTimestamp
```

## 资源配置建议

### 小型应用（< 1000 用户）

```yaml
replicas: 2
resources:
  requests:
    cpu: 50m
    memory: 64Mi
  limits:
    cpu: 200m
    memory: 256Mi
```

### 中型应用（1000-10000 用户）

```yaml
replicas: 3
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

### 大型应用（> 10000 用户）

```yaml
replicas: 5
resources:
  requests:
    cpu: 200m
    memory: 256Mi
  limits:
    cpu: 1000m
    memory: 1Gi
```

## 安全建议

1. **使用非 root 用户**
   - 已在 `deployment.yaml` 中配置 `runAsUser: 101`

2. **限制容器权限**
   - 已配置 `allowPrivilegeEscalation: false`
   - 已配置 `readOnlyRootFilesystem: false`

3. **使用 Secret 管理敏感信息**
   ```bash
   kubectl create secret generic api-secret \
     --from-literal=api-key=your-secret-key
   ```

4. **配置网络策略**
   - 限制 Pod 之间的网络访问

5. **定期更新镜像**
   - 使用最新的安全补丁

## 监控和告警

### 配置 Prometheus 监控

```yaml
apiVersion: v1
kind: Service
metadata:
  name: svelte-admin
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "80"
    prometheus.io/path: "/metrics"
```

### 配置告警规则

```yaml
groups:
- name: svelte-admin
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
    annotations:
      summary: "High error rate detected"
```

## 故障排查

### Pod 无法启动

1. 查看 Pod 状态：`kubectl describe pod <pod-name>`
2. 查看日志：`kubectl logs <pod-name>`
3. 检查镜像是否存在
4. 检查资源配额

### 服务无法访问

1. 检查 Service：`kubectl get svc svelte-admin`
2. 检查端点：`kubectl get endpoints svelte-admin`
3. 检查 Ingress：`kubectl describe ingress svelte-admin`
4. 检查 DNS 解析

### HPA 不工作

1. 检查 Metrics Server：`kubectl top nodes`
2. 查看 HPA 状态：`kubectl describe hpa svelte-admin-hpa`
3. 检查资源指标是否正常

## 清理资源

```bash
# 删除所有资源
kubectl delete -f .

# 或使用标签删除
kubectl delete all -l app=svelte-admin
```

## 参考文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Nginx Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [HPA 文档](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
