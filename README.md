# LostLink - 校园失物招领系统

软件工程课程设计项目：

- 后端：Java 17 + Spring Boot + MyBatis + Maven
- 前端：Vue 3 + Vite + Pinia + Vue Router + Axios
- 数据库：MySQL 8+
- 文件存储：本地文件系统 (`uploads/`)

## 主要功能

- 用户注册/登录和令牌认证
- 发布失物/招领帖子
- 上传物品图片 (`<input type="file" />`)
- 提交认领申请并上传证明图片 (`<input type="file" />`)
- 查看物品详情和认领记录
- 管理员审核认领申请（批准/拒绝）
- 管理员仪表板统计

## 后端 API 概述

- 认证
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/auth/me`
- 物品
  - `GET /api/items`
  - `GET /api/items/categories`
  - `GET /api/items/{id}`
  - `POST /api/items`
  - `GET /api/items/mine`
  - `GET /api/items/{id}/claims`
  - `PUT /api/items/{id}/close`
- 认领
  - `POST /api/claims`
  - `GET /api/claims/mine`
  - `GET /api/claims` (管理员)
  - `GET /api/claims/pending` (管理员)
  - `PUT /api/claims/{id}/review` (管理员)
- 管理员
  - `GET /api/admin/stats`
- 文件
  - `POST /api/files/upload` (multipart/form-data, field name: `file`)

## 数据库说明

运行 `sql/lostfound.sql` 初始化数据库架构和种子数据。

重要列：

- `lost_item.item_type`：`LOST` 或 `FOUND`
- `lost_item.image_urls`：逗号分隔的图片 URL
- `claim_record.proof_images`：逗号分隔的证明图片 URL

## 快速开始

1. 运行数据库脚本：
   - `sql/lostfound.sql`
2. 在以下文件中配置数据库：
   - `lostfound-backend/src/main/resources/application.yml`
3. 启动后端：
   - `mvn -pl lostfound-backend spring-boot:run`
4. 启动前端：
   - `cd frontend`
   - `npm install`
   - `npm run dev`

## 演示账户

- 管理员：`admin / admin123`
- 用户：`user01 / user123`
