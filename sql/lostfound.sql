CREATE DATABASE IF NOT EXISTS lostfound DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lostfound;

DROP TABLE IF EXISTS claim_record;
DROP TABLE IF EXISTS lost_item;
DROP TABLE IF EXISTS user_account;

CREATE TABLE user_account
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    real_name     VARCHAR(50)  NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lost_item
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(1000),
    image_urls   TEXT,
    category     VARCHAR(50),
    location     VARCHAR(120),
    lost_time    DATETIME,
    contact      VARCHAR(120),
    item_type    VARCHAR(20)  NOT NULL DEFAULT 'LOST',
    publisher_id BIGINT       NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lost_item_user FOREIGN KEY (publisher_id) REFERENCES user_account (id)
);

CREATE TABLE claim_record
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id           BIGINT        NOT NULL,
    claimant_id       BIGINT        NOT NULL,
    claim_reason      VARCHAR(1000) NOT NULL,
    proof_description VARCHAR(1000),
    proof_images      TEXT,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    reviewer_id       BIGINT        NULL,
    review_note       VARCHAR(500),
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at       DATETIME      NULL,
    CONSTRAINT fk_claim_item FOREIGN KEY (item_id) REFERENCES lost_item (id),
    CONSTRAINT fk_claim_user FOREIGN KEY (claimant_id) REFERENCES user_account (id)
);

INSERT INTO user_account (username, password_hash, real_name, phone, role)
VALUES ('admin', 'admin123', '系统管理员', '18800000000', 'ADMIN'),
       ('user01', 'user123', '演示用户', '18811111111', 'USER'),
       ('user02', 'user123', '物品发布者', '18822222222', 'USER');

INSERT INTO lost_item (title, description, category, location, lost_time, contact, item_type, publisher_id, status)
VALUES ('蓝色钱包', '一个蓝色的钱包，内含学生卡。', '钱包', '图书馆门口', '2026-04-10 14:30:00', '微信:user01', 'FOUND',
        3, 'OPEN'),
       ('黑色耳机', '黑色小盒子装的无线耳机。', '数码产品', '食堂二楼', '2026-04-08 12:00:00', '电话:18811111111',
        'LOST', 3, 'OPEN');
