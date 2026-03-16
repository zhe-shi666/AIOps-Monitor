-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: aiops_monitor
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `incident_log`
--

DROP TABLE IF EXISTS `incident_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `incident_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `metric_name` varchar(50) NOT NULL COMMENT '指标名称 (CPU/MEMORY)',
  `metric_value` double NOT NULL COMMENT '触发时的实际数值',
  `threshold` double NOT NULL COMMENT '设定的告警阈值',
  `message` varchar(255) DEFAULT NULL COMMENT '告警详细描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2982 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `incident_log`
--

LOCK TABLES `incident_log` WRITE;
/*!40000 ALTER TABLE `incident_log` DISABLE KEYS */;
INSERT INTO `incident_log` VALUES (1,'CPU',8.435153852942227,5,'系统资源紧张，CPU 已超过设定阈值 5.0%','2026-03-15 05:38:41'),(2,'MEMORY',83.31612698261812,80,'系统资源紧张，MEMORY 已超过设定阈值 80.0%','2026-03-15 05:38:42'),(3,'MEMORY',83.48918276296386,80,'系统资源紧张，MEMORY 已超过设定阈值 80.0%','2026-03-15 05:38:46'),(4,'CPU',6.462264541067457,5,'系统资源紧张，CPU 已超过设定阈值 5.0%','2026-03-15 07:09:03'),(5,'MEMORY',88.6107633290849,80,'系统资源紧张，MEMORY 已超过设定阈值 80.0%','2026-03-15 07:09:03'),(6,'CPU',5.345086271567892,5,'系统资源紧张，CPU 已超过设定阈值 5.0%','2026-03-15 07:09:08');
/*!40000 ALTER TABLE `incident_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_metrics_history`
--

DROP TABLE IF EXISTS `system_metrics_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_metrics_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cpu_usage` double NOT NULL,
  `mem_usage` double NOT NULL,
  `timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_metrics_history`
--

LOCK TABLES `system_metrics_history` WRITE;
/*!40000 ALTER TABLE `system_metrics_history` DISABLE KEYS */;
INSERT INTO `system_metrics_history` VALUES (1,9.16789330607854,89.44612394326032,'2026-03-15 15:30:59'),(2,9.602049103517212,90.84285269370903,'2026-03-15 15:31:03'),(3,7.80065689742294,90.83669893222233,'2026-03-15 15:31:08'),(4,6.166687429924007,90.56380712710644,'2026-03-15 15:31:13'),(5,8.180866136881662,90.61896584287146,'2026-03-15 15:31:18');
/*!40000 ALTER TABLE `system_metrics_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-16 18:58:49
