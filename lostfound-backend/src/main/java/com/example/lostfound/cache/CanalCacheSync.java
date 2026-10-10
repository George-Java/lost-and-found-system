package com.example.lostfound.cache;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.example.lostfound.config.CanalProperties;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * 订阅 MySQL binlog：数据库一旦发生增删改，立刻清理 Redis 与 JVM 进程缓存，保证两级缓存不会读到旧数据。
 * 只监听真正被缓存的表（当前是 lost_item），其他表没有缓存也就无需处理。
 */
@Component
public class CanalCacheSync {

    private static final Logger log = LoggerFactory.getLogger(CanalCacheSync.class);

    private final CanalProperties properties;
    private final TwoLevelCache cache;
    private volatile boolean running = true;
    private Thread worker;

    public CanalCacheSync(CanalProperties properties, TwoLevelCache cache) {
        this.properties = properties;
        this.cache = cache;
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Canal 缓存同步已关闭 (lostfound.canal.enabled=false)");
            return;
        }
        worker = new Thread(this::consume, "canal-cache-sync");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    /** 连接失败不影响启动，后台按间隔重连。 */
    private void consume() {
        while (running) {
            CanalConnector connector = null;
            try {
                connector = CanalConnectors.newSingleConnector(
                        new InetSocketAddress(properties.getHost(), properties.getPort()),
                        properties.getDestination(), "", "");
                connector.connect();
                connector.subscribe(properties.getFilter());
                connector.rollback();
                log.info("Canal 已连接 {}:{}, destination={}, filter={}",
                        properties.getHost(), properties.getPort(), properties.getDestination(), properties.getFilter());
                while (running) {
                    poll(connector);
                }
            } catch (Exception ex) {
                if (running) {
                    log.warn("Canal 消费中断({})，{}ms 后重连", ex.getMessage(), properties.getReconnectDelay());
                    sleep(properties.getReconnectDelay());
                }
            } finally {
                disconnect(connector);
            }
        }
    }

    private void poll(CanalConnector connector) throws Exception {
        Message message = connector.getWithoutAck(properties.getBatchSize());
        long batchId = message.getId();
        if (batchId == -1 || message.getEntries().isEmpty()) {
            sleep(500);
            return;
        }
        try {
            message.getEntries().forEach(this::handleEntry);
            connector.ack(batchId);
        } catch (Exception ex) {
            log.warn("binlog 批次处理失败，回滚后重来: {}", ex.getMessage());
            connector.rollback(batchId);
        }
    }

    private void handleEntry(CanalEntry.Entry entry) {
        if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
            return;
        }
        String table = entry.getHeader().getTableName();
        if (!"lost_item".equals(table)) {
            return;
        }
        CanalEntry.EventType eventType = entry.getHeader().getEventType();
        try {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                evictItemDetail(rowData.getBeforeColumnsList());
                evictItemDetail(rowData.getAfterColumnsList());
            }
            cache.evictByPrefix(CacheKeys.ITEM_PAGE_PREFIX);
            cache.evict(CacheKeys.ITEM_CATEGORIES);
            log.info("Canal 检测到 lost_item 变更({})，已清理物品详情/列表/分类缓存", eventType);
        } catch (InvalidProtocolBufferException ex) {
            log.warn("binlog 数据解析失败: {}", ex.getMessage());
        }
    }

    private void evictItemDetail(Iterable<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            if ("id".equals(column.getName()) && !column.getIsNull() && !column.getValue().isEmpty()) {
                cache.evict(CacheKeys.itemDetail(Long.valueOf(column.getValue())));
            }
        }
    }

    private void disconnect(CanalConnector connector) {
        if (connector == null) {
            return;
        }
        try {
            connector.disconnect();
        } catch (Exception ex) {
            log.debug("关闭 Canal 连接失败: {}", ex.getMessage());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
