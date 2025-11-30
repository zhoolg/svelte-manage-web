# ============================================================
# Makefile - 简化常用操作
# ============================================================

# 变量定义
IMAGE_NAME := your-registry/svelte-admin
IMAGE_TAG := latest
NAMESPACE := default
DEPLOYMENT_NAME := svelte-admin

# 默认目标
.DEFAULT_GOAL := help

# 帮助信息
.PHONY: help
help:
	@echo "可用命令："
	@echo "  make build          - 构建 Docker 镜像"
	@echo "  make push           - 推送镜像到仓库"
	@echo "  make deploy         - 部署到 K8s"
	@echo "  make update         - 更新 K8s 部署"
	@echo "  make rollback       - 回滚到上一版本"
	@echo "  make scale          - 扩缩容（需指定 REPLICAS）"
	@echo "  make logs           - 查看日志"
	@echo "  make status         - 查看部署状态"
	@echo "  make clean          - 清理 K8s 资源"
	@echo "  make dev            - 本地开发模式"
	@echo "  make test-docker    - 本地测试 Docker"

# 构建镜像
.PHONY: build
build:
	@echo "构建 Docker 镜像..."
	docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "构建完成！"

# 推送镜像
.PHONY: push
push: build
	@echo "推送镜像到仓库..."
	docker push $(IMAGE_NAME):$(IMAGE_TAG)
	@echo "推送完成！"

# 部署到 K8s
.PHONY: deploy
deploy:
	@echo "部署到 Kubernetes..."
	kubectl apply -f k8s/configmap.yaml
	kubectl apply -f k8s/deployment.yaml
	kubectl apply -f k8s/service.yaml
	kubectl apply -f k8s/ingress.yaml
	kubectl apply -f k8s/hpa.yaml
	@echo "部署完成！"

# 使用 Kustomize 部署
.PHONY: deploy-kustomize
deploy-kustomize:
	@echo "使用 Kustomize 部署..."
	kubectl apply -k k8s/
	@echo "部署完成！"

# 更新部署
.PHONY: update
update: push
	@echo "更新 Kubernetes 部署..."
	kubectl set image deployment/$(DEPLOYMENT_NAME) \
		$(DEPLOYMENT_NAME)=$(IMAGE_NAME):$(IMAGE_TAG) \
		-n $(NAMESPACE)
	kubectl rollout status deployment/$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo "更新完成！"

# 回滚
.PHONY: rollback
rollback:
	@echo "回滚到上一版本..."
	kubectl rollout undo deployment/$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	kubectl rollout status deployment/$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo "回滚完成！"

# 扩缩容
.PHONY: scale
scale:
	@if [ -z "$(REPLICAS)" ]; then \
		echo "错误：请指定副本数，例如：make scale REPLICAS=5"; \
		exit 1; \
	fi
	@echo "扩缩容到 $(REPLICAS) 个副本..."
	kubectl scale deployment/$(DEPLOYMENT_NAME) --replicas=$(REPLICAS) -n $(NAMESPACE)
	@echo "扩缩容完成！"

# 查看日志
.PHONY: logs
logs:
	@echo "查看应用日志..."
	kubectl logs -f -l app=$(DEPLOYMENT_NAME) -n $(NAMESPACE) --tail=100

# 查看状态
.PHONY: status
status:
	@echo "=== Pods ==="
	kubectl get pods -l app=$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo ""
	@echo "=== Service ==="
	kubectl get svc $(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo ""
	@echo "=== Ingress ==="
	kubectl get ingress $(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo ""
	@echo "=== HPA ==="
	kubectl get hpa $(DEPLOYMENT_NAME)-hpa -n $(NAMESPACE)

# 查看详细信息
.PHONY: describe
describe:
	@echo "=== Deployment ==="
	kubectl describe deployment $(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo ""
	@echo "=== Pods ==="
	kubectl describe pods -l app=$(DEPLOYMENT_NAME) -n $(NAMESPACE)

# 清理资源
.PHONY: clean
clean:
	@echo "清理 Kubernetes 资源..."
	kubectl delete -f k8s/ --ignore-not-found=true
	@echo "清理完成！"

# 本地开发
.PHONY: dev
dev:
	@echo "启动本地开发服务器..."
	pnpm install
	pnpm dev

# 本地构建
.PHONY: build-local
build-local:
	@echo "本地构建..."
	pnpm install
	pnpm build

# 测试 Docker
.PHONY: test-docker
test-docker: build
	@echo "启动 Docker 容器测试..."
	docker run -d --name svelte-admin-test -p 8080:80 $(IMAGE_NAME):$(IMAGE_TAG)
	@echo "容器已启动，访问 http://localhost:8080"
	@echo "停止容器：docker stop svelte-admin-test"
	@echo "删除容器：docker rm svelte-admin-test"

# 使用 docker-compose 测试
.PHONY: test-compose
test-compose:
	@echo "使用 docker-compose 启动..."
	docker-compose up -d
	@echo "服务已启动，访问 http://localhost:8080"

# 停止 docker-compose
.PHONY: stop-compose
stop-compose:
	@echo "停止 docker-compose 服务..."
	docker-compose down

# 查看资源使用
.PHONY: top
top:
	@echo "=== Pod 资源使用 ==="
	kubectl top pods -l app=$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo ""
	@echo "=== Node 资源使用 ==="
	kubectl top nodes

# 进入容器
.PHONY: exec
exec:
	@echo "进入容器..."
	kubectl exec -it $$(kubectl get pod -l app=$(DEPLOYMENT_NAME) -n $(NAMESPACE) -o jsonpath='{.items[0].metadata.name}') -n $(NAMESPACE) -- sh

# 重启部署
.PHONY: restart
restart:
	@echo "重启部署..."
	kubectl rollout restart deployment/$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	kubectl rollout status deployment/$(DEPLOYMENT_NAME) -n $(NAMESPACE)
	@echo "重启完成！"

# 查看事件
.PHONY: events
events:
	@echo "查看最近事件..."
	kubectl get events -n $(NAMESPACE) --sort-by=.metadata.creationTimestamp

# 验证配置
.PHONY: validate
validate:
	@echo "验证 Kubernetes 配置..."
	kubectl apply --dry-run=client -f k8s/
	@echo "配置验证通过！"
