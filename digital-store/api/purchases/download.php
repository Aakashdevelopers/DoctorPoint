<?php
/**
 * Authorized Private File Download Endpoint
 * Endpoint: GET /api/purchases/download.php?id={purchaseId}
 */

require_once __DIR__ . '/../../config/auth.php';

$user = authenticateUser(true);

$purchaseId = sanitizeString($_GET['id'] ?? '');

if (empty($purchaseId)) {
    sendJsonResponse(['success' => false, 'error' => 'Purchase ID parameter is required.'], 400);
}

$db = getDbConnection();

$sql = "SELECT pur.*, prod.title, prod.category, prod.private_file_path, prod.file_name, prod.resource_url
        FROM purchases pur
        INNER JOIN products prod ON pur.product_id = prod.id
        WHERE pur.id = :id LIMIT 1";

$stmt = $db->prepare($sql);
$stmt->execute([':id' => $purchaseId]);
$purchase = $stmt->fetch();

if (!$purchase) {
    sendJsonResponse(['success' => false, 'error' => 'Purchase record not found.'], 404);
}

// Strict Ownership Verification
if ((int)$purchase['user_id'] !== (int)$user['id'] && !$user['is_admin']) {
    sendJsonResponse([
        'success' => false,
        'error'   => 'Forbidden. You do not have permission to download this digital file.'
    ], 403);
}

$fileName = $purchase['file_name'] ?: 'digital-product.zip';
$privateFilePath = UPLOAD_DIR_PRIVATE . ltrim($purchase['private_file_path'], '/');

if (!empty($purchase['private_file_path']) && file_exists($privateFilePath) && is_file($privateFilePath)) {
    // Clear any active output buffers
    if (ob_get_level()) {
        ob_end_clean();
    }

    header('Content-Description: File Transfer');
    header('Content-Type: application/octet-stream');
    header('Content-Disposition: attachment; filename="' . basename($fileName) . '"');
    header('Content-Length: ' . filesize($privateFilePath));
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');

    readfile($privateFilePath);
    exit;
}

// Fallback Digital Delivery File Generator if sample product file is not physically on disk
if (ob_get_level()) {
    ob_end_clean();
}

$content = "NovaStore Authorized Digital Product File\n\n";
$content .= "Title: " . $purchase['title'] . "\n";
$content .= "Category: " . $purchase['category'] . "\n";
$content .= "Resource URL: " . ($purchase['resource_url'] ?: 'https://drive.google.com/') . "\n";
$content .= "Owner User ID: " . $user['id'] . "\n";
$content .= "User Email: " . $user['email'] . "\n";
$content .= "Purchase ID: " . $purchase['id'] . "\n";
$content .= "Type: Demo Grant\n";
$content .= "Status: Active\n";

header('Content-Type: text/plain; charset=utf-8');
header('Content-Disposition: attachment; filename="' . preg_replace('/[^a-z0-9_\-\.]/i', '_', $fileName) . '.txt"');
header('Content-Length: ' . strlen($content));

echo $content;
exit;
