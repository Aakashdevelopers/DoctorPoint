<?php
/**
 * Single Product Details Endpoint
 * Endpoint: GET /api/products/detail.php?slug=...
 */

require_once __DIR__ . '/../../config/auth.php';

$slug = sanitizeString($_GET['slug'] ?? '');
$id   = sanitizeString($_GET['id'] ?? '');

if (empty($slug) && empty($id)) {
    sendJsonResponse(['success' => false, 'error' => 'Product slug or ID parameter is required.'], 400);
}

$db = getDbConnection();

if (!empty($id) && is_numeric($id)) {
    $stmt = $db->prepare("SELECT * FROM products WHERE id = :id LIMIT 1");
    $stmt->execute([':id' => (int)$id]);
} else {
    $stmt = $db->prepare("SELECT * FROM products WHERE slug = :slug LIMIT 1");
    $stmt->execute([':slug' => $slug]);
}

$product = $stmt->fetch();

if (!$product) {
    sendJsonResponse(['success' => false, 'error' => 'Product not found.'], 404);
}

// Check authorization if status is draft or archived
$user = authenticateUser(false);
if ($product['status'] !== 'published') {
    if (!$user || !$user['is_admin']) {
        sendJsonResponse(['success' => false, 'error' => 'Product is currently unavailable.'], 404);
    }
}

// Ownership Check
$isPurchased = false;
$purchaseId = null;

if ($user) {
    $purStmt = $db->prepare("SELECT id FROM purchases WHERE user_id = :uid AND product_id = :pid LIMIT 1");
    $purStmt->execute([':uid' => $user['id'], ':pid' => $product['id']]);
    $existingPurchase = $purStmt->fetch();
    if ($existingPurchase) {
        $isPurchased = true;
        $purchaseId = $existingPurchase['id'];
    }
}

// Format Image URL
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? "https" : "http";
$host = $_SERVER['HTTP_HOST'] ?? 'localhost';
$baseUrl = $protocol . "://" . $host;

$coverPath = $product['cover_image_path'];
$coverUrl = (strpos($coverPath, 'http') === 0)
    ? $coverPath
    : $baseUrl . '/uploads/public/' . ltrim($coverPath, '/');

// Decode JSON fields safely
$features = !empty($product['features']) ? json_decode($product['features'], true) : [];
$testimonials = !empty($product['testimonials']) ? json_decode($product['testimonials'], true) : [];
$faqs = !empty($product['faqs']) ? json_decode($product['faqs'], true) : [];

$fileSizeFormatted = 'Digital Download';
if ($product['file_size'] > 0) {
    if ($product['file_size'] >= 1024 * 1024) {
        $fileSizeFormatted = number_format($product['file_size'] / (1024 * 1024), 2) . ' MB';
    } else {
        $fileSizeFormatted = number_format($product['file_size'] / 1024, 2) . ' KB';
    }
}

sendJsonResponse([
    'success' => true,
    'data'    => [
        '_id'                 => $product['id'],
        'title'               => $product['title'],
        'slug'                => $product['slug'],
        'shortDescription'    => $product['short_description'],
        'description'         => $product['description'],
        'coverImageUrl'       => $coverUrl,
        'priceInINR'          => (string)number_format($product['price_in_paise'] / 100, 0, '.', ''),
        'compareAtPriceInINR' => $product['compare_at_price_in_paise'] > 0 ? (string)number_format($product['compare_at_price_in_paise'] / 100, 0, '.', '') : null,
        'category'            => $product['category'],
        'fileName'            => $product['file_name'],
        'fileSizeFormatted'   => $fileSizeFormatted,
        'resourceUrl'         => $product['resource_url'] ?: 'https://drive.google.com/',
        'supportEmail'        => $product['support_email'] ?: 'ravivadhiya46@gmail.com',
        'supportPhone'        => $product['support_phone'] ?: '+91 9574055968',
        'features'            => is_array($features) ? $features : [],
        'testimonials'        => is_array($testimonials) ? $testimonials : [],
        'faqs'                => is_array($faqs) ? $faqs : [],
        'resourceCount'       => (int)($product['resource_count'] ?? 1),
        'resourceType'        => $product['resource_type'] ?: 'File',
        'status'              => $product['status'],
        'isPurchased'         => $isPurchased,
        'purchaseId'          => $purchaseId
    ]
]);
