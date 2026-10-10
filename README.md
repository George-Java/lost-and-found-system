# LostLink - 校园失物招领系统

软件工程课程设计项目，采用前后端分离架构。

- 后端：Eclipse Temurin JDK 21 + Spring Boot 3 + Spring Security + MyBatis Plus（ServiceImpl + XML） + WebSocket + RabbitMQ + Redis + Caffeine + Canal + Maven
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
- 用户搜索、好友申请、同意后收发私信；私信持久化到数据库，刷新/重登都能看到历史，对方在线时经 WebSocket（`/ws/messages?token=...`）实时送达
- 热点读请求走 **Caffeine 进程缓存 → Redis → MySQL** 两级缓存；Canal 订阅 binlog，数据库一变更就清理两级缓存，保证缓存与库一致
- 顶部帮助按钮引导新用户快速上手
- Element Plus 现代化页面组件

## 环境

- Windows 10 Pro 或 Windows 11 Pro
- Eclipse Temurin JDK 21
- IntelliJ IDEA 2026
- Maven 3.9.12
- Node.js 20+ 推荐
- MySQL 8+（需开启 `log_bin` + `ROW` 格式，供 Canal 订阅）
- Redis 7+
- RabbitMQ 3.12+
- canal-server 1.1.8（可选，关闭方式见下文）
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
- 私信
  - `POST /api/messages`（body：`{"toUserId": 2, "content": "你好"}`，落库后立即返回，并推送在线的接收人）
  - `GET /api/messages?friendId=2&limit=200`（与某位好友的历史消息，按时间正序）
  - `WS /ws/messages?token=...`（只负责接收：连上后接收实时推送）
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
- `lost_item.status`：`PENDING`、`OPEN`、`MATCHED`、`CLOSED`、`REJECTED`、`DELETED`
- `lost_item.image_urls`：逗号分隔的图片 URL
- `claim_record.proof_images`：逗号分隔的证明材料 URL
- `friend_request`：好友申请记录
- `user_friend`：好友关系，只有互为好友后才能通信
- `chat_message`：私信记录（`from_user_id`、`to_user_id`、`content`、`sent_at`），刷新页面、重新登录都能查到历史

已有数据库升级时执行 `sql/chat-message.sql` 建私信表（新库直接跑 `sql/lostfound.sql` 即可，已包含该表）。

## 异步私信说明

私信不要求收发双方同时在线：**MySQL 保存全量消息，RabbitMQ 只负责给在线的接收人做即时推送**。

- 发送：`POST /api/messages` 校验好友关系 → 写入 `chat_message` → 投进接收人队列 → 返回带 `id` 的消息，发送方本地回显。
- 在线接收：接收人打开消息页建立 WebSocket，后端为其队列（`lostfound.chat.user.{userId}`，绑定交换机 `lostfound.chat.exchange`）启动消费者，新消息实时推送；页面全部关闭后停止消费。
- 离线接收：消息本来就在数据库里，接收人下次打开消息页时由 `GET /api/messages` 拉取历史；前端按 `id` 去重，所以"推送 + 历史"不会重复显示。
- 队列只是推送通道，上线时会清空积压，消息不会因为队列丢失而丢；请仍需 backup MySQL。
- 本地开发默认直连 CentOS 虚拟机 `192.168.68.128`（MySQL 3306 / Redis 6379 / RabbitMQ 5672，账号密码见 `application.yml`），改环境变量 `SPRING_DATASOURCE_*`、`SPRING_DATA_REDIS_*`、`SPRING_RABBITMQ_*` 即可切换。

## 项目结构

```
lostfound-backend/src/main/java/com/example/lostfound/
├── auth/          登录态与 Token 过滤器（鉴权由 SecurityConfig 的路径规则统一控制）
├── cache/         两级缓存（TwoLevelCache）与 Canal 失效同步（CanalCacheSync）
├── common/        统一响应体与全局异常处理
├── config/        安全、MyBatis-Plus、RabbitMQ、WebSocket、Canal 配置
├── controller/    REST 接口层，只依赖 Service 接口
├── domain/        领域模型
│   ├── entity/    数据库实体
│   ├── dto/       请求参数对象
│   └── vo/        返回前端 / 消息队列的对象
├── mapper/        MyBatis-Plus Mapper（复杂 SQL 见 resources/mapper/*.xml）
├── messaging/     RabbitMQ 审计事件与私信信箱
├── service/       业务接口
│   └── impl/      业务实现，继承 ServiceImpl
├── util/          工具类
└── websocket/     WebSocket 推送通道
```

## 数据访问与缓存

### 前端约定

- 所有请求走 `frontend/src/api/http.js`：业务码非 0、网络或 HTTP 错误都由响应拦截器统一弹一次提示，业务代码不必再为"提示"写 `try/catch`；确实需要自己接管错误的请求传 `{ silent: true }` 关掉提示。
- 提示过的异常会带上 `notified` 标记，`main.js` 据此不再向控制台抛未处理的 Promise 异常。

### 分层与 SQL 位置

- `entity`、`dto`、`vo` 统一放在 `domain` 包下：`entity` 是 MyBatis-Plus 实体（Lombok `@Data`），`dto`/`vo` 一律写成 **Java 21 `record`**（不可变、无样板代码，Jackson 与参数校验都原生支持）；Controller 只做参数校验与组装，业务查询全部走 Service；
- 每个实体一个业务接口 + 实现：`UserService`/`UserServiceImpl`、`ItemService`/`ItemServiceImpl`、`ClaimService`/`ClaimServiceImpl`、`UserFriendService`/`UserFriendServiceImpl`、`FriendRequestService`/`FriendRequestServiceImpl`、`ChatMessageService`/`ChatMessageServiceImpl`，认领审核由 `ClaimReviewService`/`ClaimReviewServiceImpl` 承担；接口放在 `service` 包，实现放在 `service.impl` 包并继承 MyBatis-Plus 的 `ServiceImpl<Mapper, Entity>`，Controller 只注入接口；Service 实现与 Controller 统一用 Lombok `@RequiredArgsConstructor` 注入依赖，不再手写构造器（`FileController` 因参数带 `@Value` 保留手写构造器）；
- 简单条件查询用 `LambdaQueryWrapper` 写在实现类内，复杂 SQL 一律写在 XML，项目内**不使用注解写 SQL**：
  - `lostfound-backend/src/main/resources/mapper/ItemMapper.xml`：动态条件分页 `selectItemPage`、分类 `selectCategories`、统计 `countByType` / `countByCategory`
  - `lostfound-backend/src/main/resources/mapper/ClaimMapper.xml`：认领详情/列表（关联 `user_account`、`lost_item`）、审核相关更新
- `application.yml` 配置了 `mybatis-plus.mapper-locations: classpath*:/mapper/**/*.xml` 与分页插件，XML 方法带 `IPage` 参数即可自动分页。

### 两级缓存（Caffeine + Redis）

读请求顺序为 **JVM 进程缓存 Caffeine → Redis → MySQL**，命中下层会回填上层：

| 缓存键                                | 内容             | TTL     |
|---------------------------------------|------------------|---------|
| `lostfound:item:detail:{id}`          | 物品详情         | 10 分钟 |
| `lostfound:item:page:{md5(查询参数)}` | 物品列表分页结果 | 10 分钟 |
| `lostfound:item:categories`           | 分类列表         | 10 分钟 |

- Caffeine 最多 10000 条、写入 10 分钟后过期；同一份 JSON 同步写入 Redis，进程重启后仍可命中。
- 配置项：`lostfound.cache.ttl`、`lostfound.cache.local-max-size`。
- 实现见 `cache/TwoLevelCache.java`，业务侧一行 `cache.get(key, type, () -> 查库)` 即可。

### 缓存一致性（Canal 订阅 binlog）

1. **应用内写路径**：发布/审核/关闭/删除物品、认领通过等操作会立即清掉相关键（`ItemService#evict`），正常业务无延迟。
2. **Canal 兜底**：`cache/CanalCacheSync.java` 订阅 canal-server，只要 `lostfound` 库发生增删改（包括绕过应用的直接 SQL、其它实例/服务的写入），立刻清理 Caffeine 与 Redis：
   - `lost_item` 变更 → 清 `detail:{id}`（取变更前后行的 id）+ 全部列表键 + 分类键
   - 其它表当前未被缓存，无需处理；将来新增缓存表时在 `handleEntry` 里加一个分支即可

canal-server 与 MySQL 前置条件（本项目即按此部署）：

```bash
docker run -d --name canal --restart unless-stopped -p 11111:11111 \
  -e canal.destinations=heima \
  -e canal.instance.master.address=192.168.68.128:3306 \
  -e canal.instance.dbUsername=canal -e canal.instance.dbPassword=123456 \
  -e "canal.instance.filter.regex=redis_cache\..*,lostfound\..*" \
  -e canal.instance.connectionCharset=UTF-8 -e canal.instance.tsdb.enable=true \
  canal/canal-server:v1.1.8
```

- MySQL 侧要求：`log_bin=ON`、`binlog_format=ROW`、`binlog_row_image=FULL`、唯一 `server_id`，并准备带复制权限的账号：
  `CREATE USER 'canal'@'%' IDENTIFIED BY '123456'; GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';`
- 一个 canal-server 实例只能配一套 MySQL 连接参数，因此让 `heima` 这个 destination 同时过滤 `redis_cache` 与 `lostfound`，多个项目共用；本应用客户端只订阅 `lostfound\..*`，不会收到其它库的事件。
- 应用侧配置项：`lostfound.canal.*`；不使用 Canal 时设 `LOSTFOUND_CANAL_ENABLED=false`，回退为仅应用内清缓存。
- Canal 连不上时只打印告警并每 3 秒重连，不影响应用启动与其它功能。

### 自测缓存是否生效

```bash
# 1) 连续两次请求，第二次应明显更快
curl -s "http://localhost:8080/api/items/1" > /dev/null && curl -s "http://localhost:8080/api/items/1" > /dev/null
# 2) 查看 Redis 中的缓存键
redis-cli -a 123456 --no-auth-warning keys 'lostfound:item:*'
# 3) 绕过应用直接改库，观察日志与接口返回值：应立刻返回新值
mysql -uroot -p123456 lostfound -e "update lost_item set title='缓存一致性验证' where id=1"
# 应用日志出现：Canal 检测到 lost_item 变更(UPDATE)，已清理物品详情/列表/分类缓存
```

## 快速开始

1. 初始化数据库：
   - 执行 `sql/lostfound.sql`（已有库只需补 `sql/chat-message.sql`）
2. 准备中间件（默认连虚拟机 `192.168.68.128`，可改 `application.yml` 或环境变量）：
   - MySQL 8（`log_bin` + `ROW`）、Redis 7、RabbitMQ 3.12
   - canal-server（见上文命令）；不想装就设 `LOSTFOUND_CANAL_ENABLED=false`
3. 启动后端：
   - `mvn -pl lostfound-backend spring-boot:run`
4. 启动前端开发服务：
   - `cd frontend`
   - `npm install`
   - `npm run dev`
5. 访问：
   - 开发环境：http://localhost:5173

启动日志出现 `Canal 已连接 192.168.68.128:11111, destination=heima, filter=lostfound\..*` 说明缓存同步链路已就绪。

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

单镜像方案：一个容器内同时运行 Nginx（前端 + 反向代理）和 Spring Boot，`docker-compose.yml` 同时拉起 MySQL、Redis、RabbitMQ 三个依赖。

### 构建与启动

```bash
cp .env.example .env      # 按提示改成强密码，chmod 600 .env
docker compose up -d --build
docker compose ps
```

- 前端访问：http://localhost（`HTTP_PORT` 可改）
- 后端 API 由 Nginx 代理到 `/api`，WebSocket 代理到 `/ws/messages`
- MySQL 首次启动会自动执行 `sql/lostfound.sql` 与 `sql/production-indexes.sql`
- 应用通过环境变量连接各中间件（见 `docker-compose.yml` 中的 `SPRING_DATASOURCE_*`、`SPRING_DATA_REDIS_*`、`SPRING_RABBITMQ_*`）

### 缓存同步在容器环境中的配置

Compose 没有对外发布 MySQL 端口，canal-server 若要在容器里订阅 binlog，需要接入同一个 compose 网络（或另外发布 3306 端口）：

```bash
docker run -d --name canal --restart unless-stopped --network <compose项目名>_default -p 11111:11111 \
  -e canal.destinations=heima \
  -e canal.instance.master.address=mysql:3306 \
  -e canal.instance.dbUsername=canal -e canal.instance.dbPassword=123456 \
  -e "canal.instance.filter.regex=lostfound\..*" \
  -e canal.instance.connectionCharset=UTF-8 -e canal.instance.tsdb.enable=true \
  canal/canal-server:v1.1.8
```

容器内应用访问 `canal-server` 时把 `LOSTFOUND_CANAL_HOST` 设为容器名；不想用 Canal 就设 `LOSTFOUND_CANAL_ENABLED=false`，缓存仍会由应用内写路径清理。

### 镜像推送（可选）

```bash
docker tag lostfound:latest your-registry/lostfound:latest
docker push your-registry/lostfound:latest
```

### 常见问题

- **MySQL 绑定地址**：若数据库在宿主机而非 compose 中，需确保 MySQL 监听 `0.0.0.0`（`bind-address = 0.0.0.0`），并把 `SPRING_DATASOURCE_URL` 的主机改成 `host.docker.internal` 或宿主机 IP。
- **Redis 密码**：`SPRING_DATA_REDIS_PASSWORD` 必须与 compose 中 Redis 的 `--requirepass` 一致，否则 Redisson 启动即失败。
- **Canal 前置条件**：MySQL 需 `log_bin=ON` + `binlog_format=ROW`，并存在带 `REPLICATION SLAVE/CLIENT` 权限的账号，否则 canal 实例会一直报 `should start first` / 认证失败。

## 演示账户

- 管理员：`admin / admin123`
- 用户：`user01 / user123`
