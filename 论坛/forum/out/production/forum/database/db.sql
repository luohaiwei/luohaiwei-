create database bbs;

use bbs;

create table users
(
    userId int primary key auto_increment,
    userName varchar(200),
    userEmail varchar(200) not null unique,
    userPassword varchar(200),
    userGender varchar(200),
    isBanned BOOLEAN DEFAULT false
);

create table admin
(
    adminId int primary key auto_increment,
    adminCode varchar(200),
    adminPassword varchar(200)
);

create table article
(
    articleId int primary key auto_increment,
    articleTitle text,
    articleTime datetime,
    articleContent text,
    userId int,
    isActive INT DEFAULT 1 COMMENT '状态标识：1=活跃，0=禁用',
    foreign key (userId) references users(userId) on delete  cascade
);

create table reply
(
    replyId INT PRIMARY KEY AUTO_INCREMENT,
    replyContent TEXT NOT NULL,
    replyTime VARCHAR(50) NOT NULL,
    articleId INT NOT NULL,
    FOREIGN KEY (articleId) REFERENCES article(articleId) ON DELETE CASCADE
);

create table replyRelation
(
    replyRelationId int primary key auto_increment,
    userId int,
    articleId int,
    replyId int,
    foreign key (userId) references users(userId) on delete  cascade,
    foreign key (articleId) references article(articleId) on delete  cascade,
    foreign key (replyId) references reply(replyId) on delete  cascade
);

CREATE TABLE article_likes (
    like_id INT AUTO_INCREMENT PRIMARY KEY,
    article_id INT NOT NULL,
    user_id INT NOT NULL,
    like_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_like (article_id, user_id)
);

select * from users;

select * from admin;

describe article;
DROP TABLE IF EXISTS article_likes;
# 插入管理员
INSERT INTO admin (adminCode, adminPassword) VALUES ('admin', 'admin123');

select * from article;

DROP TABLE IF EXISTS article_likes;

insert into article value  (null , 'xxxxxx' , now() , 'china china china' , 1);

describe reply;

select * from reply;
select * from replyRelation;
delete from article;
delete from reply;

select r.* from reply r inner join replyRelation re on r.replyId = re.replyId and re.articleId = 21;
select count(r.replyId) from reply r inner join replyRelation re on r.replyId = re.replyId and re.articleId = 21;

select * from reply;
select * from replyRelation;

delete from reply;
delete from replyRelation;
