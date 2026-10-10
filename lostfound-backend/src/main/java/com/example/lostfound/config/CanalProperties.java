package com.example.lostfound.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Canal 客户端配置：连接 canal-server，订阅 MySQL binlog 变更。 */
@Data
@ConfigurationProperties(prefix = "lostfound.canal")
public class CanalProperties {
    private boolean enabled = true;
    private String host = "127.0.0.1";
    private int port = 11111;
    private String destination = "lostfound";
    private String filter = "lostfound\\..*";
    private int batchSize = 100;
    /** 断线重连间隔（毫秒）。 */
    private long reconnectDelay = 3000L;
}
