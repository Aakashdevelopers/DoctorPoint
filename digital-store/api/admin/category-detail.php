<?php
/**
 * Admin Category Detail Endpoint (Update & Delete)
 * Endpoint: POST /api/admin/categories/{id} | PUT /api/admin/categories/{id} | DELETE /api/admin/categories/{id}
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

$id = sanitizeString($_GET['id'] ?? '');

if (empty($id) || !is_numeric($id)) {
    sendJsonResponse(['success' => false, 'error' => 'Valid category ID parameter is required.'], 400);
}

// Check category exists
$stmt = $db->prepare("SELECT * FROM categories WHERE id = :id LIMIT 1");
$stmt->execute([':id' => $id]);
$category = $stmt->fetch();

if (!$category) {
    sendJsonResponse(['success' => false, 'error' => 'Category not found.'], 404);
}

$method = $_SERVER['REQUEST_METHOD'];

// Handle Category Rename / Update
if ($method === 'PUT' || $method === 'POST') {
    $rawInput = json_decode(file_get_contents('php://input'), true) ?: [];
    $name = sanitizeString($_POST['name'] ?? $rawInput['name'] ?? '');

    if (empty($name) || strlen($name) < 2) {
        sendJsonResponse(['success' => false, 'error' => 'Category name must be at least 2 characters long.'], 400);
    }

    if (strlen($name) > 100) {
        sendJsonResponse(['success' => false, 'error' => 'Category name cannot exceed 100 characters.'], 400);
    }

    $slug = generateSlug($name);

    // Check duplicate name or slug on OTHER categories
    $checkStmt = $db->prepare("SELECT id FROM categories WHERE (LOWER(name) = LOWER(:name) OR slug = :slug) AND id != :id LIMIT 1");
    $checkStmt->execute([':name' => $name, ':slug' => $slug, ':id' => $id]);
    if ($checkStmt->fetch()) {
        sendJsonResponse(['success' => false, 'error' => 'Another category with this name already exists.'], 400);
    }

    $oldName = $category['name'];

    $updStmt = $db->prepare("UPDATE categories SET name = :name, slug = :slug WHERE id = :id");
    $updStmt->execute([':name' => $name, ':slug' => $slug, ':id' => $id]);

    // Optionally update category name in products table where matching
    if ($oldName !== $name) {
        $prodStmt = $db->prepare("UPDATE products SET category = :newName WHERE category = :oldName");
        $prodStmt->execute([':newName' => $name, ':oldName' => $oldName]);
    }

    sendJsonResponse([
        'success' => true,
        'message' => 'Category updated successfully.',
        'data'    => ['_id' => $id, 'name' => $name, 'slug' => $slug]
    ]);
}

// Handle Category Deletion
if ($method === 'DELETE') {
    $delStmt = $db->prepare("DELETE FROM categories WHERE id = :id");
    $delStmt->execute([':id' => $id]);

    sendJsonResponse([
        'success' => true,
        'message' => 'Category deleted successfully.'
    ]);
}
