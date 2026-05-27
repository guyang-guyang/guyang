<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db.php';

// 从 system_config 表中获取分享配置（复用现有表，新增字段）
$stmt = $pdo->query("SELECT 
    COALESCE(share_title, '发现一款超好用的软件盒') as share_title,
    COALESCE(share_text, '顾阳软件盒-海量应用免费下载，每日更新') as share_text,
    COALESCE(share_image_url, '') as share_image_url,
    COALESCE(share_link, '') as share_link,
    COALESCE(share_reward_points, 30) as share_reward_points
    FROM system_config LIMIT 1");

$row = $stmt->fetch(PDO::FETCH_ASSOC);

if ($row) {
    echo json_encode([
        'code' => 0,
        'data' => $row
    ], JSON_UNESCAPED_UNICODE);
} else {
    // 默认配置
    echo json_encode([
        'code' => 0,
        'data' => [
            'share_title' => '发现一款超好用的软件盒',
            'share_text' => '顾阳软件盒-海量应用免费下载，每日更新',
            'share_image_url' => '',
            'share_link' => 'http://47.108.209.71',
            'share_reward_points' => 30
        ]
    ], JSON_UNESCAPED_UNICODE);
}
?>