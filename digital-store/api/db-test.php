<?php
/**
 * Database Connection & Environment Diagnostic Test Script
 * Endpoint: /api/db-test.php
 */

require_once __DIR__ . '/../config/config.php';

try {
    $db = getDbConnection();

    // Query MySQL table counts
    $tables = ['users', 'categories', 'products', 'purchases', 'admin_audit_logs'];
    $tableCounts = [];

    foreach ($tables as $table) {
        try {
            $stmt = $db->query("SELECT COUNT(*) AS total FROM `" . $table . "`");
            $row = $stmt->fetch();
            $tableCounts[$table] = (int)$row['total'];
        } catch (Exception $e) {
            $tableCounts[$table] = 'TABLE NOT FOUND (' . $e->getMessage() . ')';
        }
    }

    sendJsonResponse([
        'success'      => true,
        'message'      => 'MySQL Database connection successful!',
        'server_info'  => [
            'php_version'   => PHP_VERSION,
            'pdo_driver'    => $db->getAttribute(PDO::ATTR_DRIVER_NAME),
            'server_ver'    => $db->getAttribute(PDO::ATTR_SERVER_VERSION),
            'upload_public' => is_writable(UPLOAD_DIR_PUBLIC) ? 'Writable' : 'Not Writable',
            'upload_priv'   => is_writable(UPLOAD_DIR_PRIVATE) ? 'Writable' : 'Not Writable'
        ],
        'tables_found' => $tableCounts
    ]);
} catch (Exception $e) {
    sendJsonResponse([
        'success' => false,
        'error'   => 'Database test failed: ' . $e->getMessage()
    ], 500);
}
