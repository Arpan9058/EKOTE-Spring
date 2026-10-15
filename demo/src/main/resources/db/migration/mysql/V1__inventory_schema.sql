-- Initial reviewed inventory schema. Existing databases require explicit adoption.

CREATE TABLE `guns` (
  `id` int NOT NULL AUTO_INCREMENT,
  `gun_name` varchar(255) NOT NULL,
  `model` varchar(255) NOT NULL,
  `quantity` int NOT NULL,
  `registered_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_gun_name_model` (`gun_name`,`model`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `guns_details` (
  `unique_identifier` varchar(255) NOT NULL,
  `gun_name` varchar(255) DEFAULT NULL,
  `issue_date` varchar(255) DEFAULT NULL,
  `maintenance_date` varchar(255) DEFAULT NULL,
  `manufactured_date` varchar(255) DEFAULT NULL,
  `manufactured_place` varchar(255) DEFAULT NULL,
  `model` varchar(255) DEFAULT NULL,
  `next_maintenance_date` varchar(255) DEFAULT NULL,
  `quantity` int DEFAULT NULL,
  `registered_by` varchar(255) DEFAULT NULL,
  `return_date` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `user_email` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `user_role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`unique_identifier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `guns_info` (
  `unique_identifier` varchar(255) NOT NULL,
  `maintenance_date` date DEFAULT NULL,
  `maintenance_freq` int DEFAULT NULL,
  `manufactured_date` date NOT NULL,
  `manufactured_place` varchar(255) NOT NULL,
  `next_maintenance_date` date DEFAULT NULL,
  `gun_id` int NOT NULL,
  PRIMARY KEY (`unique_identifier`),
  KEY `FK3nw9v9e36r8etkjigck9r0fqd` (`gun_id`),
  CONSTRAINT `FK3nw9v9e36r8etkjigck9r0fqd` FOREIGN KEY (`gun_id`) REFERENCES `guns` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `issue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `gun_id` int DEFAULT NULL,
  `issue_date` datetime(6) DEFAULT NULL,
  `return_date` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `unique_identifier` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `register` (
  `id` bigint NOT NULL,
  `mail` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `stock_movements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` varchar(255) NOT NULL,
  `delta` int NOT NULL,
  `gun_id` int NOT NULL,
  `gun_name` varchar(255) NOT NULL,
  `item_identifier` varchar(255) DEFAULT NULL,
  `model` varchar(255) NOT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `operation` enum('ADD','ISSUE','RETURN','SUBTRACT') NOT NULL,
  `quantity_after` int NOT NULL,
  `quantity_before` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_movement_gun_time` (`gun_id`,`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
