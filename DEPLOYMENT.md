# LostLink 生产部署手册（单体 Docker Compose）

## 1. 架构与容量结论

本系统面向约 8,000 名师生，但失物招领是低频业务：日常请求以列表浏览、详情、登录为主，峰值通常出现在通知或活动时段，预期为**几十 QPS 级别**，而非高并发秒杀场景。

生产形态仍是一个 Spring Boot 单体应用（应用内含 Vue 静态文件和 Nginx），但将有状态/可靠性能力外置：

- **MySQL 8.4**：唯一业务数据源；审核操作使用本地事务与条件更新保证原子性。
- **Redis 7**：登录会话持久化、分类热点缓存、Redisson 分布式锁。应用重启不会让全部用户掉线。
- **RabbitMQ 3.13**：异步审计事件，以及私信的在线实时推送（消息本身持久化在 MySQL，队列只是推送通道，上线时会清空积压）；不放入核心写操作的事务链路。
- **Nginx**：静态资源、API 和 WebSocket 反向代理。

### 为什么不部署 Seata

Seata 解决的是**跨服务或跨数据源**的分布式事务。本项目最终是单体应用 + 单 MySQL 数据库，所有核心变更可由 InnoDB 本地事务完成；此时接入 Seata 会额外引入 Seata Server、注册/配置中心、undo_log 与全局锁，并降低可用性，却没有解决新的业务问题。因此本版本**刻意不启用 Seata**。

若将来把审核、积分、资产/赔付等拆为独立服务且一次业务操作跨多个数据库，再评估 Seata AT/Saga，并先完成幂等、补偿、超时和可观测性设计；不要仅为使用框架而引入 Seata。

## 2. 云服务器规格与网络

### 建议起步规格

- Linux：Ubuntu 22.04/24.04 LTS，x86_64
- 4 vCPU / 8 GB RAM / 80 GB SSD（可稳定支撑该规模的低频业务）
- 公网安全组仅开放：`22/tcp`（限管理员 IP）、`80/tcp`、`443/tcp`
- **不要**暴露 `3306`、`6379`、`5672`、`15672`；Compose 没有发布这些端口。

生产必须绑定域名并使用 HTTPS。文件目前存储在 Docker volume；它适用于起步阶段，但必须备份。后续建议迁移到对象存储（OSS/COS/S3）。

## 3. 首次部署

以下命令在服务器 SSH 会话中执行，`example.edu.cn` 替换为学校域名。

### 3.1 安装 Docker Engine 与 Compose 插件

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker.gpg
printf '%s\n' \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

重新登录 SSH 后确认：

```bash
docker version
docker compose version
```

### 3.2 取得代码并配置机密

```bash
sudo mkdir -p /opt/lostfound
sudo chown $USER:$USER /opt/lostfound
git clone <你的仓库地址> /opt/lostfound
cd /opt/lostfound
cp .env.example .env
```

生成四个不同的强密码并写入 `.env`：

```bash
openssl rand -base64 32
nano .env
chmod 600 .env
```

不要提交 `.env`，不要保留示例密码或演示账户 `admin/admin123`。首次进入系统后应立即重置演示账户密码，或在初始化 SQL 中移除演示数据。

### 3.3 初始化、构建与启动

```bash
# 部署前确认 Compose 变量已完整展开
docker compose config

# 首次启动会自动执行 sql/lostfound.sql 和 sql/production-indexes.sql
docker compose up -d --build

docker compose ps
docker compose logs -f lostfound
```

验证：

```bash
curl -fsS http://127.0.0.1/actuator/health
curl -I http://127.0.0.1/
```

> 初始化脚本仅在 `mysql-data` 卷为空时执行。已有生产数据时，**禁止**执行 `docker compose down -v`；新增索引应在维护窗口中手工执行 `sql/production-indexes.sql`，并先做好备份。

## 4. HTTPS（推荐 Caddy，最少配置）

应用容器已占用 80 端口。生产可将 `HTTP_PORT` 改为 `127.0.0.1:8088`，让 Caddy 作为唯一公网入口：

```bash
# .env
HTTP_PORT=127.0.0.1:8088
```

```bash
sudo apt-get install -y caddy
sudo tee /etc/caddy/Caddyfile > /dev/null <<'EOF'
example.edu.cn {
    reverse_proxy 127.0.0.1:8088
}
EOF
sudo systemctl reload caddy
cd /opt/lostfound && docker compose up -d
```

确认 DNS A/AAAA 记录已经指向服务器并放行 80/443 后，Caddy 会自动签发与续期证书。应用已开启 forwarded headers 处理；WebSocket 会由反向代理正常转发。

## 5. 日常运维

### 发布更新

```bash
cd /opt/lostfound
git pull --ff-only
docker compose up -d --build
docker image prune -f
docker compose ps
```

### 查看状态和日志

```bash
docker compose ps
docker compose logs --tail=200 lostfound
docker compose logs --tail=200 mysql redis rabbitmq
curl -fsS http://127.0.0.1:8088/actuator/health
```

### 备份（至少每日一次，异机保存）

```bash
cd /opt/lostfound
mkdir -p backups
set -a; . ./.env; set +a
docker compose exec -T mysql mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" \
  --single-transaction --routines --triggers lostfound | gzip > "backups/lostfound-$(date +%F).sql.gz"
docker run --rm -v lost-and-found-system_uploads-data:/data -v "$PWD/backups":/backup alpine \
  sh -c 'tar czf /backup/uploads-'"$(date +%F)"'.tgz -C /data .'
```

先在测试机验证恢复，再将备份上传到校内备份服务器或对象存储。Docker volume 名称可能因项目目录不同而变化，先通过 `docker volume ls` 确认。

## 6. 上线前检查清单

- [ ] `.env` 全部使用强随机密码，权限为 `600`。
- [ ] 使用正式域名和 HTTPS；安全组未暴露 MySQL/Redis/RabbitMQ 管理端口。
- [ ] 禁用或重置演示账号；建立至少两个独立管理员账号。
- [ ] 上传文件类型、病毒扫描、敏感内容审核按学校政策补齐；当前本地磁盘存储不应作为长期归档。
- [ ] 已配置每日数据库和 uploads 备份，并完成过一次恢复演练。
- [ ] 使用 k6/JMeter 对登录、列表、发帖、审核分别做压测；监控 p95 延迟、MySQL 连接池、Redis 内存、RabbitMQ 堆积和磁盘使用率。
- [ ] 定期更新基础镜像与依赖，观察 `docker compose logs`、`/actuator/health`。
