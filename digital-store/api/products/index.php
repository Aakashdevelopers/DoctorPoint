<?php
/**
 * Public Products Listing Endpoint
 * Endpoint: GET /api/products?category=...&search=...
 */

require_once __DIR__ . '/../../config/config.php';

$category = sanitizeString($_GET['category'] ?? 'All');
$search   = sanitizeString($_GET['search'] ?? '');

$db = getDbConnection();

$whereConditions = ["status = 'published'"];
$params = [];

if (!empty($category) && strcasecmp($category, 'All') !== 0) {
    $whereConditions[] = "LOWER(category) = LOWER(:category)";
    $params[':category'] = $category;
}

if (!empty($search)) {
    $whereConditions[] = "(title LIKE :search OR short_description LIKE :search OR category LIKE :search)";
    $params[':search'] = '%' . $search . '%';
}

$whereClause = implode(' AND ', $whereConditions);

$sql = "SELECT id, title, slug, short_description, cover_image_path, price_in_paise, compare_at_price_in_paise, category, created_at
        FROM products
        WHERE {$whereClause}
        ORDER BY created_at DESC";

$stmt = $db->prepare($sql);
$stmt->execute($params);
$rows = $stmt->fetchAll();

$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
$host = $_SERVER['HTTP_HOST'] ?? 'localhost';
$baseUrl = $protocol . "://" . $host;

$formatted = array_map(function($p) use ($baseUrl) {
    $coverPath = $p['cover_image_path'];
    $coverUrl = (strpos($coverPath, 'http') === 0)
        ? $coverPath
        : $baseUrl . '/uploads/public/' . ltrim($coverPath, '/');

    return [
        '_id'                 => $p['id'],
        'title'               => $p['title'],
        'slug'                => $p['slug'],
        'shortDescription'    => $p['short_description'],
        'coverImageUrl'       => $coverUrl,
        'priceInINR'          => (string)number_format($p['price_in_paise'] / 100, 0, '.', ''),
        'compareAtPriceInINR' => $p['compare_at_price_in_paise'] > 0 ? (string)number_format($p['compare_at_price_in_paise'] / 100, 0, '.', '') : null,
        'category'            => $p['category'],
        'createdAt'           => $p['created_at']
    ];
}, $rows);

sendJsonResponse([
    'success' => true,
    'count'   => count($formatted),
    'data'    => $formatted
]);
