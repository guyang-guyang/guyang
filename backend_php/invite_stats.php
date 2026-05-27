<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db.php';

// 验证Token
$headers = getallheaders();
$auth = $headers['Authorization'] ?? '';
$token = str_replace('Bearer ', '', $auth);

if (empty($token)) {
    echo json_encode(['code' => 401, 'message' => '请先登录'], JSON_UNESCAPED_UNICODE);
    exit;
}

$stmt = $pdo->prepare("SELECT id, username, invite_code, has_shared FROM users WHERE token = ?");
$stmt->execute([$token]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user) {
    echo json_encode(['code' => 401, 'message' => 'Token无效'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 生成邀请码（如果没有）
if (empty($user['invite_code'])) {
    $inviteCode = strtoupper(substr(md5($user['id'] . time()), 0, 6));
    $stmt = $pdo->prepare("UPDATE users SET invite_code = ? WHERE id = ?");
    $stmt->execute([$inviteCode, $user['id']]);
    $user['invite_code'] = $inviteCode;
}

// 统计邀请人数
$stmt = $pdo->prepare("SELECT COUNT(*) as count FROM invite_records WHERE inviter_id = ?");
$stmt->execute([$user['id']]);
$inviteCount = $stmt->fetch(PDO::FETCH_ASSOC)['count'];

// 邀请列表
$stmt = $pdo->prepare("
    SELECT ir.invitee_id, ir.created_at, u.username 
    FROM invite_records ir 
    LEFT JOIN users u ON u.id = ir.invitee_id 
    WHERE ir.inviter_id = ? 
    ORDER BY ir.created_at DESC 
    LIMIT 20
");
$stmt->execute([$user['id']]);
$inviteList = $stmt->fetchAll(PDO::FETCH_ASSOC);

// 分享奖励配置
$configStmt = $pdo->query("SELECT COALESCE(share_reward_points, 30) as reward FROM system_config LIMIT 1");
$reward = intval($configStmt->fetch(PDO::FETCH_ASSOC)['reward'] ?? 30);

echo json_encode([
    'code' => 0,
    'data' => [
        'invite_code' => $user['invite_code'],
        'invite_count' => $inviteCount,
        'invite_reward' => $reward,
        'has_shared' => (bool)$user['has_shared'],
        'invite_list' => $inviteList
    ]
], JSON_UNESCAPED_UNICODE);
?>