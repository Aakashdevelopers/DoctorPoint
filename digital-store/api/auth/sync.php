<?php
/**
 * Auth Sync API Endpoint
 * Endpoint: POST /api/auth/sync
 */

require_once __DIR__ . '/../../config/auth.php';

$user = authenticateUser(true);

$displayName = sanitizeString($_POST['displayName'] ?? '');
if (empty($displayName)) {
    $rawInput = json_decode(file_get_contents('php://input'), true);
    if (isset($rawInput['displayName'])) {
        $displayName = sanitizeString($rawInput['displayName']);
    }
}

if (!empty($displayName) && $displayName !== $user['display_name']) {
    $db = getDbConnection();
    $stmt = $db->prepare("UPDATE users SET display_name = :name WHERE id = :id");
    $stmt->execute([':name' => $displayName, ':id' => $user['id']]);
    $user['display_name'] = $displayName;
}

sendJsonResponse([
    'success' => true,
    'data'    => [
        'id'          => $user['id'],
        'uid'         => $user['firebase_uid'],
        'email'       => $user['email'],
        'displayName' => $user['display_name'],
        'role'        => $user['role'],
        'isAdmin'     => $user['is_admin']
    ]
]);
