-- ============================================
-- 顾阳分发平台 2.0 数据库迁移脚本
-- 执行方式: mysql -u用户名 -p 数据库名 < migrate.sql
-- ============================================

-- 1. users 表增加字段
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS has_shared TINYINT DEFAULT 0 COMMENT '是否已完成强制分享',
    ADD COLUMN IF NOT EXISTS invite_code VARCHAR(10) COMMENT '用户邀请码',
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) COMMENT '头像URL',
    ADD COLUMN IF NOT EXISTS qq VARCHAR(20) COMMENT 'QQ号(仅获取头像用)',
    ADD INDEX IF NOT EXISTS idx_invite_code (invite_code),
    ADD INDEX IF NOT EXISTS idx_has_shared (has_shared);

-- 2. 分享记录表
CREATE TABLE IF NOT EXISTS share_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    share_type VARCHAR(20) DEFAULT 'app' COMMENT 'qq_friend/qq_zone/copy/app',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 邀请记录表
CREATE TABLE IF NOT EXISTS invite_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inviter_id VARCHAR(50) NOT NULL,
    invitee_id VARCHAR(50) NOT NULL,
    invite_code VARCHAR(10) NOT NULL,
    reward_granted TINYINT DEFAULT 0 COMMENT '奖励是否已发放',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_inviter (inviter_id),
    INDEX idx_invitee (invitee_id),
    INDEX idx_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 积分日志表
CREATE TABLE IF NOT EXISTS points_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    points INT NOT NULL,
    type ENUM('earn','spend') NOT NULL,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. system_config 表增加分享配置字段
ALTER TABLE system_config 
    ADD COLUMN IF NOT EXISTS share_title VARCHAR(200) DEFAULT '发现一款超好用的软件盒',
    ADD COLUMN IF NOT EXISTS share_text VARCHAR(500) DEFAULT '顾阳软件盒-海量应用免费下载，每日更新',
    ADD COLUMN IF NOT EXISTS share_image_url VARCHAR(500) DEFAULT '',
    ADD COLUMN IF NOT EXISTS share_link VARCHAR(500) DEFAULT 'http://47.108.209.71',
    ADD COLUMN IF NOT EXISTS share_reward_points INT DEFAULT 30;

-- 6. info 表增加网盘字段
ALTER TABLE info 
    ADD COLUMN IF NOT EXISTS pan_type VARCHAR(20) DEFAULT 'other' COMMENT 'baidu/lanzou/aliyun/quark/other',
    ADD COLUMN IF NOT EXISTS pan_code VARCHAR(50) DEFAULT '' COMMENT '网盘提取码',
    ADD COLUMN IF NOT EXISTS pan_type_name VARCHAR(50) DEFAULT '网盘' COMMENT '网盘中文名';