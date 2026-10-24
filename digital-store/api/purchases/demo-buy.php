<?php
/**
 * Instant Demo Purchase Endpoint
 * Endpoint: POST /api/purchases/demo-buy
 */

require_once __DIR__ . '/../../config/auth.php';

$user = authenticateUser(true);

$rawInput = json_decode(file_get_contents('php://input'), true) ?: [];

$productId     = sanitizeString($_POST['productId'] ?? $rawInput['productId'] ?? '');
$customerEmail = sanitizeString($_POST['customerEmail'] ?? $rawInput['customerEmail'] ?? $user['email']);
$phoneNumber   = sanitizeString($_POST['phoneNumber'] ?? $rawInput['phoneNumber'] ?? '');

if (empty($productId)) {
    sendJsonResponse(['success' => false, 'error' => 'Product ID is required.'], 400);
}

$db = getDbConnection();

// Fetch product details from DB
$prodStmt = $db->prepare("SELECT * FROM products WHERE id = :pid LIMIT 1");
$prodStmt->execute([':pid' => $productId]);
$product = $prodStmt->fetch();

if (!$product) {
    sendJsonResponse(['success' => false, 'error' => 'Product not found.'], 404);
}

if ($product['status'] !== 'published') {
    sendJsonResponse([
        'success' => false,
        'error'   => 'This product is unavailable or not currently published for purchase.'
    ], 400);
}

// Check if user already owns this product
$purStmt = $db->prepare("SELECT * FROM purchases WHERE user_id = :uid AND product_id = :pid LIMIT 1");
$purStmt->execute([':uid' => $user['id'], ':pid' => $product['id']]);
$existingPurchase = $purStmt->fetch();

if ($existingPurchase) {
    sendJsonResponse([
        'success'      => true,
        'alreadyOwned' => true,
        'message'      => 'Product already present in your purchases library.',
        'data'         => [
            'purchaseId'   => $existingPurchase['id'],
            'purchaseType' => $existingPurchase['purchase_type'],
            'status'       => $existingPurchase['status'],
            'resourceUrl'  => $product['resource_url'] ?: 'https://drive.google.com/',
            'createdAt'    => $existingPurchase['created_at']
        ]
    ]);
}

// Create new purchase access grant in MySQL
try {
    $insStmt = $db->prepare("
        INSERT INTO purchases
        (user_id, firebase_uid, product_id, user_email, phone_number, purchase_type, status, price_snapshot_in_paise)
        VALUES
        (:user_id, :firebase_uid, :product_id, :user_email, :phone_number, 'demo', 'granted', :price)
    ");

    $insStmt->execute([
        ':user_id'      => $user['id'],
        ':firebase_uid' => $user['firebase_uid'],
        ':product_id'   => $product['id'],
        ':user_email'   => !empty($customerEmail) ? $customerEmail : $user['email'],
        ':phone_number' => $phoneNumber,
        ':price'        => $product['price_in_paise']
    ]);

    $purchaseId = $db->lastInsertId();

    sendJsonResponse([
        'success'      => true,
        'alreadyOwned' => false,
        'message'      => 'Demo purchase granted successfully!',
        'data'         => [
            'purchaseId'   => $purchaseId,
            'productId'    => $product['id'],
            'productTitle' => $product['title'],
            'resourceUrl'  => $product['resource_url'] ?: 'https://drive.google.com/',
            'purchaseType' => 'demo',
            'status'       => 'granted',
            'createdAt'    => date('Y-m-d H:i:s'),
            'demoNotice'   => 'Demo Purchase — No Payment Processed'
        ]
    ], 201);
} catch (PDOException $e) {
    // Handle rare race-condition duplicate key error
    if ($e->getCode() == 23000 || strpos($e->getMessage(), '1062') !== false) {
        $purStmt->execute([':uid' => $user['id'], ':pid' => $product['id']]);
        $existing = $purStmt->fetch();

        sendJsonResponse([
            'success'      => true,
            'alreadyOwned' => true,
            'message'      => 'Product already present in your purchases library.',
            'data'         => [
                'purchaseId'   => $existing['id'] ?? null,
                'purchaseType' => 'demo',
                'status'       => 'granted',
                'resourceUrl'  => $product['resource_url'] ?: 'https://drive.google.com/'
            ]
        ]);
    }

    sendJsonResponse(['success' => false, 'error' => 'Database error granting purchase: ' . $e->getMessage()], 500);
}
