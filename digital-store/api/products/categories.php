<?php
/**
 * Public Categories Listing Endpoint
 * Endpoint: GET /api/products/categories
 */

require_once __DIR__ . '/../../config/config.php';

$db = getDbConnection();

// Fetch custom categories from categories table
$stmtCat = $db->query("SELECT name FROM categories ORDER BY name ASC");
$customCategories = $stmtCat->fetchAll(PDO::FETCH_COLUMN);

// Fetch distinct product categories from published products
$stmtProd = $db->query("SELECT DISTINCT category FROM products WHERE status = 'published'");
$prodCategories = $stmtProd->fetchAll(PDO::FETCH_COLUMN);

$merged = array_unique(array_merge($customCategories, $prodCategories));
$merged = array_values(array_filter($merged, function($c) {
    return !empty(trim($c));
}));

$finalCategories = array_merge(['All'], $merged);

sendJsonResponse([
    'success' => true,
    'data'    => $finalCategories
]);
