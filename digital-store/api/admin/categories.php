<?php
/**
 * Admin Categories Endpoint (List & Create)
 * Endpoint: GET /api/admin/categories | POST /api/admin/categories
 */

require_once __DIR__ . '/../../config/auth.php';

$admin = requireAdmin();
$db = getDbConnection();

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $stmt = $db->query("SELECT id AS _id, name, slug, created_at FROM categories ORDER BY name ASC");
    $categories = $stmt->fetchAll();

    sendJsonResponse([
        'success' => true,
        'data'    => $categories
    ]);
}

if ($method === 'POST') {
    $rawInput = json_decode(file_get_contents('php://input'), true) ?: [];
    $name = sanitizeString($_POST['name'] ?? $rawInput['name'] ?? '');

    if (empty($name) || strlen($name) < 2) {
        sendJsonResponse(['success' => false, 'error' => 'Category name must be at least 2 characters long.'], 400);
    }

    if (strlen($name) > 100) {
        sendJsonResponse(['success' => false, 'error' => 'Category name cannot exceed 100 characters.'], 400);
    }

    $slug = generateSlug($name);

    // Check duplicate name or slug
    $checkStmt = $db->prepare("SELECT id FROM categories WHERE LOWER(name) = LOWER(:name) OR slug = :slug LIMIT 1");
    $checkStmt->execute([':name' => $name, ':slug' => $slug]);
    if ($checkStmt->fetch()) {
        sendJsonResponse(['success' => false, 'error' => 'Category with this name or slug already exists.'], 400);
    }

    $insStmt = $db->prepare("INSERT INTO categories (name, slug) VALUES (:name, :slug)");
    $insStmt->execute([':name' => $name, ':slug' => $slug]);
    $newId = $db->lastInsertId();

    sendJsonResponse([
        'success' => true,
        'message' => 'Category created successfully',
        'data'    => ['_id' => $newId, 'name' => $name, 'slug' => $slug]
    ], 201);
}
