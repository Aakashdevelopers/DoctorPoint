<?php
/**
 * Admin Product Detail Endpoint (Update & Archive)
 * Endpoint: PUT /api/admin/products/{id} | DELETE /api/admin/products/{id}
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

$id = sanitizeString($_GET['id'] ?? '');

if (empty($id) || !is_numeric($id)) {
    sendJsonResponse(['success' => false, 'error' => 'Valid product ID is required.'], 400);
}

// Verify product exists
$stmt = $db->prepare("SELECT * FROM products WHERE id = :id LIMIT 1");
$stmt->execute([':id' => (int)$id]);
$product = $stmt->fetch();

if (!$product) {
    sendJsonResponse(['success' => false, 'error' => 'Product not found.'], 404);
}

$method = $_SERVER['REQUEST_METHOD'];

// Handle Product Update (PUT or POST)
if ($method === 'PUT' || $method === 'POST') {
    $rawInput = json_decode(file_get_contents('php://input'), true) ?: [];

    $title               = sanitizeString($_POST['title'] ?? $rawInput['title'] ?? $product['title']);
    $category            = sanitizeString($_POST['category'] ?? $rawInput['category'] ?? $product['category']);
    $status              = sanitizeString($_POST['status'] ?? $rawInput['status'] ?? $product['status']);
    $shortDescription    = sanitizeString($_POST['shortDescription'] ?? $rawInput['shortDescription'] ?? $product['short_description']);
    $description         = sanitizeString($_POST['description'] ?? $rawInput['description'] ?? $product['description']);
    $resourceUrl         = sanitizeString($_POST['resourceUrl'] ?? $rawInput['resourceUrl'] ?? $product['resource_url']);
    $supportEmail        = sanitizeString($_POST['supportEmail'] ?? $rawInput['supportEmail'] ?? $product['support_email']);
    $supportPhone        = sanitizeString($_POST['supportPhone'] ?? $rawInput['supportPhone'] ?? $product['support_phone']);
    $resourceType        = sanitizeString($_POST['resourceType'] ?? $rawInput['resourceType'] ?? $product['resource_type']);
    $resourceCount       = (int)($_POST['resourceCount'] ?? $rawInput['resourceCount'] ?? $product['resource_count']);

    if (empty($title) || empty($shortDescription) || empty($description)) {
        sendJsonResponse(['success' => false, 'error' => 'Title, short description, and full description cannot be empty.'], 400);
    }

    $priceInINR          = isset($_POST['priceInINR']) ? (float)$_POST['priceInINR'] : (isset($rawInput['priceInINR']) ? (float)$rawInput['priceInINR'] : ($product['price_in_paise'] / 100));
    $compareAtPriceInINR = isset($_POST['compareAtPriceInINR']) ? (float)$_POST['compareAtPriceInINR'] : (isset($rawInput['compareAtPriceInINR']) ? (float)$rawInput['compareAtPriceInINR'] : ($product['compare_at_price_in_paise'] / 100));

    $priceInPaise = (int)round($priceInINR * 100);
    $compareAtPriceInPaise = (int)round($compareAtPriceInINR * 100);

    // Cover Image Replacement
    $coverImagePath = $product['cover_image_path'];
    if (isset($_FILES['coverImage'])) {
        $coverErr = $_FILES['coverImage']['error'];
        if ($coverErr === UPLOAD_ERR_OK) {
            $maxCoverSize = 10 * 1024 * 1024; // 10MB
            if ($_FILES['coverImage']['size'] > $maxCoverSize) {
                sendJsonResponse(['success' => false, 'error' => 'Cover image exceeds maximum size limit of 10MB.'], 400);
            }

            $ext = strtolower(pathinfo($_FILES['coverImage']['name'], PATHINFO_EXTENSION));
            $allowed = ['jpg', 'jpeg', 'png', 'webp'];
            if (!in_array($ext, $allowed, true)) {
                sendJsonResponse(['success' => false, 'error' => 'Invalid cover image type. Allowed: JPG, JPEG, PNG, WEBP.'], 400);
            }

            $filename = 'cover-' . time() . '-' . rand(1000, 9999) . '.' . $ext;
            if (move_uploaded_file($_FILES['coverImage']['tmp_name'], UPLOAD_DIR_PUBLIC . $filename)) {
                $coverImagePath = $filename;
            }
        } elseif ($coverErr !== UPLOAD_ERR_NO_FILE) {
            sendJsonResponse(['success' => false, 'error' => 'Cover image upload error code: ' . $coverErr], 400);
        }
    }

    // Digital File Replacement
    $privateFilePath = $product['private_file_path'];
    $originalFileName = $product['file_name'];
    $fileSize = $product['file_size'];

    if (isset($_FILES['digitalFile'])) {
        $fileErr = $_FILES['digitalFile']['error'];
        if ($fileErr === UPLOAD_ERR_OK) {
            $maxFileSize = 100 * 1024 * 1024; // 100MB
            if ($_FILES['digitalFile']['size'] > $maxFileSize) {
                sendJsonResponse(['success' => false, 'error' => 'Digital file exceeds maximum allowed size of 100MB.'], 400);
            }

            $originalFileName = basename($_FILES['digitalFile']['name']);
            $fileSize = (int)$_FILES['digitalFile']['size'];
            $ext = strtolower(pathinfo($originalFileName, PATHINFO_EXTENSION));
            $allowedDigitalExts = ['pdf', 'zip', 'rar', 'epub', 'mp3', 'mp4', 'png', 'jpg', 'jpeg', 'doc', 'docx', 'txt', 'csv'];

            if (!in_array($ext, $allowedDigitalExts, true)) {
                sendJsonResponse(['success' => false, 'error' => 'Invalid digital file type.'], 400);
            }

            $filename = 'file-' . time() . '-' . rand(1000, 9999) . '.' . $ext;

            if (move_uploaded_file($_FILES['digitalFile']['tmp_name'], UPLOAD_DIR_PRIVATE . $filename)) {
                $privateFilePath = $filename;
            }
        } elseif ($fileErr !== UPLOAD_ERR_NO_FILE) {
            sendJsonResponse(['success' => false, 'error' => 'Digital file upload error code: ' . $fileErr], 400);
        }
    }

    // Features
    $featuresRaw = $_POST['features'] ?? $rawInput['features'] ?? null;
    $featuresArray = !empty($product['features']) ? json_decode($product['features'], true) : [];
    if ($featuresRaw !== null) {
        if (is_array($featuresRaw)) {
            $featuresArray = array_values(array_filter(array_map('trim', $featuresRaw)));
        } elseif (!empty($featuresRaw)) {
            $featuresArray = array_values(array_filter(array_map('trim', preg_split('/\r\n|\r|\n/', $featuresRaw))));
        }
    }

    // Testimonials
    $testimonialsRaw = $_POST['testimonials'] ?? $rawInput['testimonials'] ?? null;
    $testimonialsArray = !empty($product['testimonials']) ? json_decode($product['testimonials'], true) : [];
    if ($testimonialsRaw !== null) {
        $testimonialsArray = is_array($testimonialsRaw) ? $testimonialsRaw : json_decode($testimonialsRaw, true);
    }

    // FAQs
    $faqsRaw = $_POST['faqs'] ?? $rawInput['faqs'] ?? null;
    $faqsArray = !empty($product['faqs']) ? json_decode($product['faqs'], true) : [];
    if ($faqsRaw !== null) {
        $faqsArray = is_array($faqsRaw) ? $faqsRaw : json_decode($faqsRaw, true);
    }

    $updateSql = "UPDATE products SET
                  title = :title,
                  short_description = :short_desc,
                  description = :desc,
                  cover_image_path = :cover_path,
                  private_file_path = :file_path,
                  file_name = :file_name,
                  file_size = :file_size,
                  resource_url = :res_url,
                  price_in_paise = :price,
                  compare_at_price_in_paise = :compare_price,
                  category = :cat,
                  support_email = :sup_email,
                  support_phone = :sup_phone,
                  resource_count = :res_count,
                  resource_type = :res_type,
                  features = :features,
                  testimonials = :testimonials,
                  faqs = :faqs,
                  status = :status
                  WHERE id = :id";

    $updateStmt = $db->prepare($updateSql);
    $updateStmt->execute([
        ':title'         => $title,
        ':short_desc'    => $shortDescription,
        ':desc'          => $description,
        ':cover_path'    => $coverImagePath,
        ':file_path'     => $privateFilePath,
        ':file_name'     => $originalFileName,
        ':file_size'     => $fileSize,
        ':res_url'       => $resourceUrl,
        ':price'         => $priceInPaise,
        ':compare_price' => $compareAtPriceInPaise,
        ':cat'           => $category,
        ':sup_email'     => $supportEmail,
        ':sup_phone'     => $supportPhone,
        ':res_count'     => $resourceCount,
        ':res_type'      => $resourceType,
        ':features'      => json_encode(is_array($featuresArray) ? $featuresArray : [], JSON_UNESCAPED_UNICODE),
        ':testimonials'  => json_encode(is_array($testimonialsArray) ? $testimonialsArray : [], JSON_UNESCAPED_UNICODE),
        ':faqs'          => json_encode(is_array($faqsArray) ? $faqsArray : [], JSON_UNESCAPED_UNICODE),
        ':status'        => $status,
        ':id'            => $id
    ]);

    // Log admin audit action
    try {
        $auditStmt = $db->prepare("INSERT INTO admin_audit_logs (admin_user_id, admin_email, action, target_type, target_id, details) VALUES (:uid, :email, 'UPDATE_PRODUCT', 'PRODUCT', :pid, :details)");
        $auditStmt->execute([
            ':uid'     => $admin['id'],
            ':email'   => $admin['email'],
            ':pid'     => $id,
            ':details' => "Updated product: {$title}"
        ]);
    } catch (Exception $e) {}

    sendJsonResponse([
        'success' => true,
        'message' => 'Product updated successfully'
    ]);
}

// Handle Product Archiving (DELETE)
if ($method === 'DELETE') {
    $archStmt = $db->prepare("UPDATE products SET status = 'archived', archived_at = NOW() WHERE id = :id");
    $archStmt->execute([':id' => $id]);

    // Log admin audit action
    try {
        $auditStmt = $db->prepare("INSERT INTO admin_audit_logs (admin_user_id, admin_email, action, target_type, target_id, details) VALUES (:uid, :email, 'ARCHIVE_PRODUCT', 'PRODUCT', :pid, :details)");
        $auditStmt->execute([
            ':uid'     => $admin['id'],
            ':email'   => $admin['email'],
            ':pid'     => $id,
            ':details' => "Archived product ID {$id}"
        ]);
    } catch (Exception $e) {}

    sendJsonResponse([
        'success' => true,
        'message' => 'Product safely archived. Historical purchase records preserved.'
    ]);
}
