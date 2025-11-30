# ============================================================
# 多阶段构建 Dockerfile
# 阶段1: 构建阶段 - 编译 Svelte 应用
# 阶段2: 生产阶段 - 使用 Nginx 提供静态文件服务
# ============================================================

# ============================================================
# 阶段 1: 构建阶段
# ============================================================
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 pnpm-lock.yaml
COPY package.json pnpm-lock.yaml* ./

# 安装 pnpm
RUN npm install -g pnpm

# 安装依赖（利用 Docker 缓存层）
RUN pnpm install --frozen-lockfile

# 复制源代码
COPY . .

# 构建应用
RUN pnpm run build

# ============================================================
# 阶段 2: 生产阶段
# ============================================================
FROM nginx:1.27-alpine

# 安装 tzdata 用于时区设置
RUN apk add --no-cache tzdata

# 设置时区为上海
ENV TZ=Asia/Shanghai

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置文件
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 创建日志目录
RUN mkdir -p /var/log/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
