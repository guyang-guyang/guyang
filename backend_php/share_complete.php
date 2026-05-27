<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') exit;

require_once '../db.php';

// 验证Token
$headers = getallheaders();
$auth = $headers['Authorization'] ?? '';
$token = str_replace('Bearer ', '', $auth);

if (empty($token)) {
    echo json_encode(['code' => 401, 'message' => '请先登录'], JSON_UNESCAPED_UNICODE);
    exit;
}

// 查找用户
$stmt = $pdo->prepare("SELECT id, username, has_shared FROM users WHERE token = ?");
$stmt->execute([$token]);
$user = $stmt->fetch(PDO::FETCH_ASSOC);

if (!$user) {
    echo json_encode(['code' => 401, 'message' => 'Token无效'], JSON_UNESCAPED_UNICODE);
    exit;
}

$userId = $user['id'];

// 如果已分享过，直接返回
if ($user['has_shared']) {
    echo json_encode([
        'code' => 0,
        'message' => '已完成分享',
        'data' => ['points_added' => 0, 'has_shared' => true]
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// 获取分享奖励积分
$configStmt = $pdo->query("SELECT COALESCE(share_reward_points, 30) as reward FROM system_config LIMIT 1");
$config = $configStmt->fetch(PDO::FETCH_ASSOC);
$reward = intval($config['reward'] ?? 30);

// 标记已分享 + 奖励积分
$pdo->beginTransaction();
try {
    $stmt = $pdo->prepare("UPDATE users SET has_shared = 1, points = points + ? WHERE id = ?");
    $stmt->execute([$reward, $userId]);

    // 记录分享日志
    $shareType = $_POST['share_type'] ?? 'app';
    $stmt = $pdo->prepare("INSERT INTO share_records (user_id, share_type, created_at) VALUES (?, ?, NOW())");
    $stmt->execute([$userId, $shareType]);

    // 记录积分变更
    $stmt = $pdo->prepare("INSERT INTO points_log (user_id, points, type, description, created_at) VALUES (?, ?, 'earn', ?, NOW())");
    $stmt->execute([$userId, $reward, '分享奖励']);

    $pdo->commit();

    echo json_encode([
        'code' => 0,
        'message' => '分享成功',
        'data' => [
            'points_added' => $reward,
            'has_shared' => true
        ]
    ], JSON_UNESCAPED_UNICODE);
} catch (Exception $e) {
    $pdo->rollBack();
    echo json_encode(['code' => 500, 'message' => '操作失败'], JSON_UNESCAPED_UNICODE);
}
?>