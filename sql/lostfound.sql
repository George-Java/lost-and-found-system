CREATE DATABASE IF NOT EXISTS lostfound DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lostfound;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS claim_record;
DROP TABLE IF EXISTS friend_request;
DROP TABLE IF EXISTS user_friend;
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
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
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

CREATE TABLE friend_request
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT      NOT NULL,
    receiver_id  BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  DATETIME    NULL,
    CONSTRAINT fk_friend_request_requester FOREIGN KEY (requester_id) REFERENCES user_account (id),
    CONSTRAINT fk_friend_request_receiver FOREIGN KEY (receiver_id) REFERENCES user_account (id)
);

CREATE TABLE user_friend
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    friend_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_friend_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_user_friend_friend FOREIGN KEY (friend_id) REFERENCES user_account (id),
    CONSTRAINT uk_user_friend_pair UNIQUE (user_id, friend_id)
);

INSERT INTO user_account (username, password_hash, real_name, phone, role)
VALUES ('admin', 'admin123', '赵一鸣', '18800000000', 'ADMIN'),
       ('user01', 'user123', '林雨晴', '18811111111', 'USER'),
       ('user02', 'user123', '陈思远', '18822222222', 'USER');

INSERT INTO lost_item (title, description, category, location, lost_time, contact, item_type, publisher_id, status)
VALUES ('蓝色钱包', '一个蓝色的钱包，内含学生卡。', '钱包', '图书馆门口', '2026-04-10 14:30:00', '微信:user02', 'FOUND',
        3, 'OPEN'),
       ('黑色耳机', '黑色小盒子装的无线耳机。', '数码产品', '食堂二楼', '2026-04-08 12:00:00', '电话:18811111111',
        'LOST', 2, 'OPEN');

INSERT INTO friend_request (requester_id, receiver_id, status, reviewed_at)
VALUES (2, 3, 'APPROVED', CURRENT_TIMESTAMP);

INSERT INTO user_friend (user_id, friend_id)
VALUES (2, 3),
       (3, 2);
