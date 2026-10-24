<?php
/**
 * Admin Demo Purchases Audit Log Endpoint
 * Endpoint: GET /api/admin/purchases
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

$sql = "SELECT pur.id AS purchase_id, pur.user_id, pur.firebase_uid, pur.user_email,
               pur.phone_number, pur.purchase_type, pur.status, pur.created_at,
               prod.title AS product_title, prod.category, prod.price_in_paise
        FROM purchases pur
        LEFT JOIN products prod ON pur.product_id = prod.id
        ORDER BY pur.created_at DESC";

$stmt = $db->query($sql);
$rows = $stmt->fetchAll();

$formatted = array_map(function($p) {
    return [
        'purchaseId'   => $p['purchase_id'],
        'userId'       => $p['user_id'],
        'firebaseUid'  => $p['firebase_uid'],
        'userEmail'    => $p['user_email'] . (!empty($p['phone_number']) ? ' (' . $p['phone_number'] . ')' : ''),
        'productTitle' => $p['product_title'] ?: 'Archived Product',
        'category'     => $p['category'] ?: 'N/A',
        'purchaseType' => 'demo',
        'status'       => strtoupper($p['status']),
        'createdAt'    => $p['created_at'],
        'demoLabel'    => 'Demo Purchase — No Payment Processed'
    ];
}, $rows);

sendJsonResponse([
    'success' => true,
    'count'   => count($formatted),
    'data'    => $formatted
]);
