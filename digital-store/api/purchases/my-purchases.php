<?php
/**
 * User My Purchases Endpoint
 * Endpoint: GET /api/purchases/my-purchases
 */

require_once __DIR__ . '/../../config/auth.php';

$user = authenticateUser(true);

$db = getDbConnection();

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
$host = $_SERVER['HTTP_HOST'] ?? 'localhost';
$baseUrl = $protocol . "://" . $host;

$sql = "SELECT pur.id AS purchase_id, pur.purchase_type, pur.status AS purchase_status,
               pur.price_snapshot_in_paise, pur.created_at AS purchase_date,
               prod.id AS product_id, prod.title, prod.slug, prod.short_description,
               prod.category, prod.file_name, prod.file_size, prod.resource_url, prod.cover_image_path
        FROM purchases pur
        INNER JOIN products prod ON pur.product_id = prod.id
        WHERE pur.user_id = :uid
        ORDER BY pur.created_at DESC";

$stmt = $db->prepare($sql);
$stmt->execute([':uid' => $user['id']]);
$rows = $stmt->fetchAll();

$formatted = array_map(function($p) use ($baseUrl) {
    $coverPath = $p['cover_image_path'];
    $coverUrl = (strpos($coverPath, 'http') === 0)
        ? $coverPath
        : $baseUrl . '/uploads/public/' . ltrim($coverPath, '/');

    $fileSizeFormatted = 'Digital Download';
    if ($p['file_size'] > 0) {
        if ($p['file_size'] >= 1024 * 1024) {
            $fileSizeFormatted = number_format($p['file_size'] / (1024 * 1024), 2) . ' MB';
        } else {
            $fileSizeFormatted = number_format($p['file_size'] / 1024, 2) . ' KB';
        }
    }

    return [
        'purchaseId'   => $p['purchase_id'],
        'purchaseType' => $p['purchase_type'],
        'status'       => $p['purchase_status'],
        'purchaseDate' => $p['purchase_date'],
        'pricePaidINR' => (string)number_format($p['price_snapshot_in_paise'] / 100, 0, '.', ''),
        'demoNotice'   => 'Demo Purchase — No Payment Processed',
        'product'      => [
            '_id'               => $p['product_id'],
            'title'             => $p['title'],
            'slug'              => $p['slug'],
            'shortDescription'  => $p['short_description'],
            'category'          => $p['category'],
            'fileName'          => $p['file_name'],
            'fileSizeFormatted' => $fileSizeFormatted,
            'resourceUrl'       => $p['resource_url'] ?: 'https://drive.google.com/',
            'coverImageUrl'     => $coverUrl
        ]
    ];
}, $rows);

sendJsonResponse([
    'success' => true,
    'count'   => count($formatted),
    'data'    => $formatted
]);
