# LostLink - 校园失物招领系统

软件工程课程设计项目，采用前后端分离架构。

- 后端：Eclipse Temurin JDK 21 + Spring Boot 3 + Spring Security + MyBatis Plus + WebSocket + Maven
- 前端：Vue 3 + Vite + Pinia + Vue Router + Axios + Element Plus
- 数据库：MySQL 8+
- 反向代理：Nginx
- 文件存储：本地文件系统 (`uploads/`)

## 主要功能

- 用户注册、登录、退出和 Bearer Token 认证
- BCrypt 密码加密；旧演示明文密码在登录成功后会自动升级为 BCrypt
- 普通用户发布失物/招领帖子，上传物品图片，帖子需管理员审核后展示
- 普通用户提交认领申请并上传图片、PDF、Word、TXT 等证明材料
- 管理员审核寻物/招领帖子和认领申请；管理员不能发布帖子或提交认领
- 管理员仪表板统计和用户角色管理
- 用户搜索、好友申请、同意后通信；WebSocket 地址：`/ws/messages?token=...`
- 顶部帮助按钮引导新用户快速上手
- Element Plus 现代化页面组件

## 环境

- Windows 10 Pro 或 Windows 11 Pro
- Eclipse Temurin JDK 21
- IntelliJ IDEA 2026
- Maven 3.9.12
- Node.js 20+ 推荐
- MySQL 8+
- Nginx 1.24+ 推荐

## 后端 API 概览

- 认证
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/logout`
  - `GET /api/auth/me`
- 物品
  - `GET /api/items`
  - `GET /api/items/categories`
  - `GET /api/items/{id}`
  - `POST /api/items`
  - `GET /api/items/mine`
  - `GET /api/items/{id}/claims`
  - `PUT /api/items/{id}/close`
  - `PUT /api/items/{id}/open`
- 认领
  - `POST /api/claims`
  - `GET /api/claims/mine`
  - `GET /api/claims`（管理员）
  - `GET /api/claims/pending`（管理员）
  - `PUT /api/claims/{id}/review`
- 用户
  - `GET /api/users/contacts`
  - `GET /api/users/search`
  - `POST /api/users/friend-requests`
  - `GET /api/users/friend-requests/incoming`
  - `GET /api/users/friend-requests/outgoing`
  - `PUT /api/users/friend-requests/{id}/review`
- 管理员
  - `GET /api/admin/stats`
  - `GET /api/admin/users`
  - `PUT /api/admin/users/{id}/role`
  - `GET /api/admin/items/pending`
  - `PUT /api/admin/items/{id}/review`
- 文件
  - `POST /api/files/upload`（multipart/form-data，字段名：`file`）

## 数据库说明

运行 `sql/lostfound.sql` 初始化数据库架构和种子数据。

重要列：

- `lost_item.item_type`：`LOST` 或 `FOUND`
- `lost_item.status`：`PENDING`、`OPEN`、`MATCHED`、`CLOSED`、`REJECTED`
- `lost_item.image_urls`：逗号分隔的图片 URL
- `claim_record.proof_images`：逗号分隔的证明材料 URL
- `friend_request`：好友申请记录
- `user_friend`：好友关系，只有互为好友后才能通信

## 快速开始

1. 初始化数据库：
   - 执行 `sql/lostfound.sql`
2. 配置数据库连接：
   - `lostfound-backend/src/main/resources/application.yml`
3. 启动后端：
   - `mvn -pl lostfound-backend spring-boot:run`
4. 启动前端开发服务：
   - `cd frontend`
   - `npm install`
   - `npm run dev`
5. 访问：
   - 开发环境：http://localhost:5173

Vite 开发环境已代理 `/api`、`/uploads` 和 `/ws` 到 `http://localhost:8080`，前端代码不再写死后端地址。

## Nginx 部署

1. 构建前端：
   - `cd frontend`
   - `npm run build`
2. 启动后端：
   - `mvn -pl lostfound-backend spring-boot:run`
3. 将 `nginx/lostfound.conf` 放入 Nginx 配置目录并按本机路径调整 `root`。
4. 启动或重载 Nginx。

`nginx/lostfound.conf` 已包含：

- 前端单页应用路由回退：`try_files $uri $uri/ /index.html`
- `/api/` 反向代理到 Spring Boot
- `/uploads/` 反向代理到本地上传资源
- `/ws/` WebSocket Upgrade 代理

## Docker 打包与运行

项目提供了：

- 后端镜像构建文件：`lostfound-backend/Dockerfile`
- 前端镜像构建文件：`frontend/Dockerfile`
- 前端容器 Nginx 配置：`frontend/nginx.conf`
- 一键编排：`docker-compose.yml`

使用远程镜像一键运行：

```bash
docker compose pull
docker compose up -d
```

访问：

- 前端：http://localhost
- 后端通过前端 Nginx 代理访问 `/api`
- WebSocket 通过 `/ws/messages` 代理到后端

维护者构建并推送镜像示例：

```bash
docker build -f lostfound-backend/Dockerfile -t your-registry/lostfound-backend:0.1.0 .
docker build -f frontend/Dockerfile -t your-registry/lostfound-frontend:0.1.2 .
docker push your-registry/lostfound-backend:0.1.0
docker push your-registry/lostfound-frontend:0.1.2
```

## 演示账户

- 管理员：`admin / admin123`
- 用户：`user01 / user123`
