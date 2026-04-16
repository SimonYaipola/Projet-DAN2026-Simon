-- CraftBoard.sql
-- Schéma complet CraftBoard (MariaDB)
CREATE DATABASE IF NOT EXISTS craftboard;
USE craftboard;
-- ======================
-- CITY
-- ======================
CREATE TABLE city (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(120) NOT NULL,
api_url VARCHAR(255),
code VARCHAR(16) NOT NULL UNIQUE,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ======================
-- USER
-- ======================
CREATE TABLE app_user (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,

username VARCHAR(80) NOT NULL,
password_hash VARCHAR(255) NOT NULL,
role VARCHAR(30) NOT NULL DEFAULT 'USER',

display_name VARCHAR(120),

-- Avatar en BLOB
avatar LONGBLOB,

created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT uq_user_city UNIQUE (city_id, username),
CONSTRAINT fk_user_city FOREIGN KEY (city_id) REFERENCES city(id)
);

-- ======================
-- POLE
-- ======================
CREATE TABLE pole (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,
name VARCHAR(120) NOT NULL,

CONSTRAINT uq_pole_city_name UNIQUE (city_id, name),
CONSTRAINT fk_pole_city FOREIGN KEY (city_id) REFERENCES city(id)
);

-- ======================
-- ORDER
-- ======================
CREATE TABLE craft_order (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,

created_by BIGINT NOT NULL,
pole_id BIGINT NOT NULL,

status VARCHAR(30) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_order_city FOREIGN KEY (city_id) REFERENCES city(id),
CONSTRAINT fk_order_user FOREIGN KEY (created_by) REFERENCES app_user(id),
CONSTRAINT fk_order_pole FOREIGN KEY (pole_id) REFERENCES pole(id)
);

-- ======================
-- ORDER ITEMS
-- ======================
CREATE TABLE order_item (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
order_id BIGINT NOT NULL,

category VARCHAR(30) NOT NULL,
tier VARCHAR(30) NOT NULL,
rarity VARCHAR(30) NOT NULL,
quantity INT NOT NULL,

tool_type VARCHAR(30),
equipment_type VARCHAR(30),
armor_material VARCHAR(30),

CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES craft_order(id),
CONSTRAINT ck_item_qty CHECK (quantity > 0)
);

-- ======================
-- EQUIPMENT
-- ======================
CREATE TABLE equipment (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,

owner_id BIGINT NOT NULL,
pole_id BIGINT,

name VARCHAR(120) NOT NULL,
category VARCHAR(30) NOT NULL,
tier VARCHAR(30),
rarity VARCHAR(30),

condition_value INT DEFAULT 0,

created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_equipment_city FOREIGN KEY (city_id) REFERENCES city(id),
CONSTRAINT fk_equipment_owner FOREIGN KEY (owner_id) REFERENCES app_user(id),
CONSTRAINT fk_equipment_pole FOREIGN KEY (pole_id) REFERENCES pole(id),
CONSTRAINT ck_equipment_condition CHECK (condition_value >= 0)
);

-- ======================
-- QUEST
-- ======================
CREATE TABLE quest (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,

title VARCHAR(120) NOT NULL,
description TEXT,
status VARCHAR(30) NOT NULL,

created_by BIGINT,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_quest_city FOREIGN KEY (city_id) REFERENCES city(id),
CONSTRAINT fk_quest_user FOREIGN KEY (created_by) REFERENCES app_user(id)
);

-- ======================
-- ACTIVITY LOG
-- ======================
CREATE TABLE activity_log (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
city_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,

action_type VARCHAR(50) NOT NULL,
description TEXT,

created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_log_city FOREIGN KEY (city_id) REFERENCES city(id),
CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
