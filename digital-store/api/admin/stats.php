<?php
/**
 * Admin Dashboard Stats Endpoint
 * Endpoint: GET /api/admin/stats
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

// Query Product Counters
$totalProducts = (int)$db->query("SELECT COUNT(*) FROM products")->fetchColumn();
$publishedProducts = (int)$db->query("SELECT COUNT(*) FROM products WHERE status = 'published'")->fetchColumn();
$draftProducts = (int)$db->query("SELECT COUNT(*) FROM products WHERE status = 'draft'")->fetchColumn();
$archivedProducts = (int)$db->query("SELECT COUNT(*) FROM products WHERE status = 'archived'")->fetchColumn();

// Query Purchases Counter
$totalDemoPurchases = (int)$db->query("SELECT COUNT(*) FROM purchases WHERE purchase_type = 'demo'")->fetchColumn();

// Query Recent Demo Activity Audit Log
$recentSql = "SELECT pur.id AS purchase_id, pur.user_email, pur.purchase_type, pur.status, pur.created_at,
                     prod.title AS product_title
              FROM purchases pur
              LEFT JOIN products prod ON pur.product_id = prod.id
              ORDER BY pur.created_at DESC
              LIMIT 5";

$stmt = $db->query($recentSql);
$recentRows = $stmt->fetchAll();

$recentActivity = array_map(function($p) {
    return [
        'purchaseId'   => $p['purchase_id'],
        'userEmail'    => $p['user_email'] ?: 'Customer',
        'productTitle' => $p['product_title'] ?: 'Archived Product',
        'purchaseType' => $p['purchase_type'],
        'status'       => $p['status'],
        'date'         => $p['created_at'],
        'demoLabel'    => 'Demo Purchase — No Payment Processed'
    ];
}, $recentRows);

sendJsonResponse([
    'success' => true,
    'data'    => [
        'totalProducts'      => $totalProducts,
        'publishedProducts'  => $publishedProducts,
        'draftProducts'      => $draftProducts,
        'archivedProducts'   => $archivedProducts,
        'totalDemoPurchases' => $totalDemoPurchases,
        'recentActivity'     => $recentActivity
    ]
]);
