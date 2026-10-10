-- 私信持久化：消息落 MySQL，刷新/重登/换设备都能查到历史；
-- RabbitMQ 只负责给在线的接收人做即时推送。
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_general_ci NOT NULL,
  `sent_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_from_to_sent` (`from_user_id`,`to_user_id`,`sent_at`),
  KEY `idx_chat_to_from_sent` (`to_user_id`,`from_user_id`,`sent_at`),
  CONSTRAINT `fk_chat_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_chat_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
