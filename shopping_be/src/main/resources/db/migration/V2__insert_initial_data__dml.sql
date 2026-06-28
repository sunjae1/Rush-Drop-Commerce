-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 3.39.147.146    Database: myshopping
-- ------------------------------------------------------
-- Server version	8.0.45-0ubuntu0.24.04.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Dumping data for table `cart`
--

LOCK TABLES `cart` WRITE;
/*!40000 ALTER TABLE `cart`
    DISABLE KEYS */;
INSERT IGNORE INTO `cart` (`id`, `user_id`)
VALUES (4, 1);
/*!40000 ALTER TABLE `cart`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `cart_item`
--

LOCK TABLES `cart_item` WRITE;
/*!40000 ALTER TABLE `cart_item`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_item`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `category`
--

LOCK TABLES `category` WRITE;
/*!40000 ALTER TABLE `category`
    DISABLE KEYS */;
/*!40000 ALTER TABLE `category`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment`
    DISABLE KEYS */;
INSERT IGNORE INTO `comment` (`created_date`, `id`, `post_id`, `user_id`, `content`)
VALUES ('2025-11-17 22:39:33.860219', 1, 1, 1, '테스트용 댓글 입력 중입니다. \n테스트 테스트  테스트 테스트 '),
       ('2025-11-17 22:40:38.620565', 2, 1, 1, 'asd'),
       ('2026-03-06 17:54:30.423350', 4, 1, 1, '하하이');
/*!40000 ALTER TABLE `comment`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `item`
--

LOCK TABLES `item` WRITE;
/*!40000 ALTER TABLE `item`
    DISABLE KEYS */;
INSERT IGNORE INTO `item` (`deleted`, `price`, `quantity`, `category_id`, `id`, `image_url`, `item_name`)
VALUES (_binary '\0', 2000, 7, 1, 1, '/image/1.webp', '남성 스투시 반팔 상의'),
       (_binary '\0', 4000, 8, 2, 2, '/image/2.webp', '남성 블루종 레더 자켓 블랙'),
       (_binary '\0', 100000, 30, 3, 3, '/image/women_coat1.jpeg', '여성 더블페이스 울 롱 코트'),
       (_binary '\0', 50000, 46, 4, 4, '/image/women_coat2.webp', '여성 머플러하프코트'),
       (_binary '\0', 50000, 50, 5, 5, '/image/man_shirt.webp', '남성 코듀로이 셔츠 브라운');
/*!40000 ALTER TABLE `item`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member`
    DISABLE KEYS */;
INSERT IGNORE INTO `member` (`id`, `password`, `email`, `name`, `active`, `role`)
VALUES (1, '{bcrypt}$2a$10$uSTp5Q8/2o2Li./gxCLuUe392rYD.JoGR8nB4y9kCILqecroFMwEq', 'test@na.com', '테스터', 1, 'ADMIN');
/*!40000 ALTER TABLE `member`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item`
    DISABLE KEYS */;
INSERT IGNORE INTO `order_item` (`order_price`, `quantity`, `id`, `item_id`, `order_id`)
VALUES (2000, 3, 1, 1, 1),
       (4000, 5, 2, 2, 2),
       (50000, 4, 3, 4, 2),
       (4000, 4, 4, 2, 3),
       (4000, 3, 5, 2, 4);
/*!40000 ALTER TABLE `order_item`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders`
    DISABLE KEYS */;
INSERT IGNORE INTO `orders` (`id`, `order_date`, `user_id`, `status`)
VALUES (1, '2025-11-17 22:39:33.821208', 1, 'PAID'),
       (2, '2025-11-18 23:02:20.896535', 1, 'PAID'),
       (3, '2025-11-19 11:16:40.335610', 1, 'PAID'),
       (4, '2025-11-19 14:12:20.587377', 1, 'PAID');
/*!40000 ALTER TABLE `orders`
    ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `post`
--

LOCK TABLES `post` WRITE;
/*!40000 ALTER TABLE `post`
    DISABLE KEYS */;
INSERT IGNORE INTO `post` (`created_date`, `id`, `user_id`, `author`, `content`, `title`)
VALUES ('2025-11-17 22:39:33.843430', 1, 1, '테스터', '테스트용 게시글입니다. 게시글 입니다.\n게시글 게시글 게시글', '첫 글 축하');
/*!40000 ALTER TABLE `post`
    ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2026-03-06 23:01:58
