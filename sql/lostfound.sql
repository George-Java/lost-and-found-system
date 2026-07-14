-- MySQL dump 10.13  Distrib 8.4.10, for Linux (x86_64)
--
-- Host: localhost    Database: lostfound
-- ------------------------------------------------------
-- Server version	8.4.10

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `claim_record`
--

DROP TABLE IF EXISTS `claim_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `claim_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint NOT NULL,
  `claimant_id` bigint NOT NULL,
  `claim_reason` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL,
  `proof_description` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `proof_images` text COLLATE utf8mb4_general_ci,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `reviewer_id` bigint DEFAULT NULL,
  `review_note` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_claim_item` (`item_id`),
  KEY `fk_claim_user` (`claimant_id`),
  CONSTRAINT `fk_claim_item` FOREIGN KEY (`item_id`) REFERENCES `lost_item` (`id`),
  CONSTRAINT `fk_claim_user` FOREIGN KEY (`claimant_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `claim_record`
--

LOCK TABLES `claim_record` WRITE;
/*!40000 ALTER TABLE `claim_record` DISABLE KEYS */;
INSERT INTO `claim_record` VALUES (1,3,3,'就是我的，没有理由','无',NULL,'APPROVED',1,'已核验身份与物品细节，允许认领','2026-07-14 20:13:44','2026-07-14 20:13:56');
/*!40000 ALTER TABLE `claim_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `friend_request`
--

DROP TABLE IF EXISTS `friend_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `friend_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `requester_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_friend_request_requester` (`requester_id`),
  KEY `fk_friend_request_receiver` (`receiver_id`),
  CONSTRAINT `fk_friend_request_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_friend_request_requester` FOREIGN KEY (`requester_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `friend_request`
--

LOCK TABLES `friend_request` WRITE;
/*!40000 ALTER TABLE `friend_request` DISABLE KEYS */;
INSERT INTO `friend_request` VALUES (1,2,3,'APPROVED','2026-07-14 18:54:49','2026-07-14 18:54:49'),(2,2,1,'APPROVED','2026-07-14 20:09:34','2026-07-14 20:09:41'),(3,3,1,'APPROVED','2026-07-14 22:34:30','2026-07-14 22:34:35');
/*!40000 ALTER TABLE `friend_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lost_item`
--

DROP TABLE IF EXISTS `lost_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lost_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `image_urls` text COLLATE utf8mb4_general_ci,
  `category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `location` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lost_time` datetime DEFAULT NULL,
  `contact` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `item_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'LOST',
  `publisher_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_lost_item_user` (`publisher_id`),
  CONSTRAINT `fk_lost_item_user` FOREIGN KEY (`publisher_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lost_item`
--

LOCK TABLES `lost_item` WRITE;
/*!40000 ALTER TABLE `lost_item` DISABLE KEYS */;
INSERT INTO `lost_item` VALUES (1,'蓝色钱包','一个蓝色的钱包，内含学生卡。',NULL,'钱包','图书馆门口','2026-04-10 14:30:00','微信:user02','FOUND',3,'DELETED','2026-07-14 18:54:49','2026-07-14 22:21:59'),(2,'黑色耳机','黑色小盒子装的无线耳机。',NULL,'数码产品','食堂二楼','2026-04-08 12:00:00','电话:18811111111','LOST',2,'DELETED','2026-07-14 18:54:49','2026-07-14 22:18:37'),(3,'图书馆内捡到的蓝牙耳机','黑色蓝牙耳机，牌子是索尼，入耳式耳机','https://imgservice.suning.cn/uimg1/b2c/image/ouIEua35jp9sToV0KCA6ig.jpg_800w_800h_4e','数码产品','图书馆一楼102室32号桌面上','2026-05-06 22:18:00','13945074310','FOUND',2,'OPEN','2026-07-14 22:20:37','2026-07-14 22:21:21'),(4,'六食堂丢失的iPhone手机','iPhone 16Pro Max白色，存储是512GB，贴有钢化膜，左上角钢化膜有小面积碎裂，锁屏壁纸是一只熊猫，手机壳是透明磁吸手机壳，手机掉漆比较严重。','https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSoxKZH9VioiYgJ4gHhgainMlIOKKNhyAJFuv1OC8F4jw&s=10','数码产品','六食堂西半区某餐桌','2026-06-05 12:02:00','14781650432','LOST',3,'OPEN','2026-07-14 22:27:53','2026-07-14 22:28:00');
/*!40000 ALTER TABLE `lost_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `password_hash` varchar(120) COLLATE utf8mb4_general_ci NOT NULL,
  `real_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_account`
--

LOCK TABLES `user_account` WRITE;
/*!40000 ALTER TABLE `user_account` DISABLE KEYS */;
INSERT INTO `user_account` VALUES (1,'admin','$2a$10$Bjdpa9FxIjaAQKYkut4I..VDEtubGo2USswSnUR81weohwIXvMW7S','赵一鸣','18800000000','ADMIN','2026-07-14 18:54:49'),(2,'user01','$2a$10$C71O8B/9mMM7RBr4wwBD3ujPWeO7iV7reqnnvQrrhuBycFEW5Ldlq','林雨晴','18811111111','USER','2026-07-14 18:54:49'),(3,'user02','$2a$10$ORaO4zYaCbirYrhU/TpgIePv0bWLMz5Rtxc5tJJs1q3ysF5YFrZrO','陈思远','18822222222','USER','2026-07-14 18:54:49');
/*!40000 ALTER TABLE `user_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_friend`
--

DROP TABLE IF EXISTS `user_friend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `friend_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_friend_pair` (`user_id`,`friend_id`),
  KEY `fk_user_friend_friend` (`friend_id`),
  CONSTRAINT `fk_user_friend_friend` FOREIGN KEY (`friend_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_user_friend_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_friend`
--

LOCK TABLES `user_friend` WRITE;
/*!40000 ALTER TABLE `user_friend` DISABLE KEYS */;
INSERT INTO `user_friend` VALUES (3,2,1,'2026-07-14 20:09:40'),(4,1,2,'2026-07-14 20:09:40'),(5,3,1,'2026-07-14 22:34:34'),(6,1,3,'2026-07-14 22:34:34');
/*!40000 ALTER TABLE `user_friend` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-14 23:17:31
