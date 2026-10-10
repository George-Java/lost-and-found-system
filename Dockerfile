# ==================== Stage 1: 构建前端 ====================
FROM node:20-alpine AS frontend-build

WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ==================== Stage 2: 构建后端 ====================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /workspace
COPY pom.xml .
COPY lostfound-backend/pom.xml lostfound-backend/pom.xml
RUN mvn -pl lostfound-backend -am dependency:go-offline -B

COPY lostfound-backend/src lostfound-backend/src
RUN mvn -pl lostfound-backend -am -DskipTests package -B

# ==================== Stage 3: 最终镜像 ====================
FROM eclipse-temurin:21-jre

ENV TZ=Asia/Shanghai
ENV LOSTFOUND_UPLOAD_DIR=/app/uploads

# 安装 nginx 和 supervisor
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/uploads \
    && rm -f /etc/nginx/sites-enabled/default

# 复制后端 jar
COPY --from=backend-build /workspace/lostfound-backend/target/lostfound-backend-0.1.0.jar /app/app.jar

# 复制前端构建产物到 nginx 目录
COPY --from=frontend-build /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY docker/nginx.conf /etc/nginx/conf.d/lostfound.conf

# 复制 supervisor 配置
COPY docker/supervisord.conf /etc/supervisor/conf.d/lostfound.conf

EXPOSE 80

CMD ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
