<?php
/**
 * Admin Product Status Update Endpoint
 * Endpoint: PATCH /api/admin/products/{id}/status
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

$id = sanitizeString($_GET['id'] ?? '');

if (empty($id) || !is_numeric($id)) {
    sendJsonResponse(['success' => false, 'error' => 'Valid product ID parameter is required.'], 400);
}

$rawInput = json_decode(file_get_contents('php://input'), true) ?: [];
$status = sanitizeString($_POST['status'] ?? $rawInput['status'] ?? '');

$allowed = ['draft', 'published', 'archived'];
if (!in_array($status, $allowed, true)) {
    sendJsonResponse(['success' => false, 'error' => 'Invalid status value. Allowed: draft, published, archived.'], 400);
}

$archivedAt = ($status === 'archived') ? date('Y-m-d H:i:s') : null;

$stmt = $db->prepare("UPDATE products SET status = :status, archived_at = :archived_at WHERE id = :id");
$stmt->execute([
    ':status'      => $status,
    ':archived_at' => $archivedAt,
    ':id'          => $id
]);

// Log admin audit action
try {
    $auditStmt = $db->prepare("INSERT INTO admin_audit_logs (admin_user_id, admin_email, action, target_type, target_id, details) VALUES (:uid, :email, 'CHANGE_STATUS', 'PRODUCT', :pid, :details)");
    $auditStmt->execute([
        ':uid'     => $admin['id'],
        ':email'   => $admin['email'],
        ':pid'     => $id,
        ':details' => "Changed status of product ID {$id} to {$status}"
    ]);
} catch (Exception $e) {}

sendJsonResponse([
    'success' => true,
    'message' => "Product status updated to {$status}"
]);
