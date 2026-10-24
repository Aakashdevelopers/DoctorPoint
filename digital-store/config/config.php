<?php
/**
 * NovaStore Digital Storefront - Master Configuration File
 * Designed for PHP 7.4+ / 8.x and InfinityFree Shared Hosting
 */

// Immediate CORS Preflight Handling (Before any script processing)
if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    header('Access-Control-Max-Age: 86400');
    exit(0);
}

// Strict Error Handling (Never display errors in output to protect credentials)
error_reporting(E_ALL);
ini_set('display_errors', '0');
ini_set('log_errors', '1');

// Base Paths
define('BASE_DIR', dirname(__DIR__));
define('UPLOAD_DIR_PUBLIC', BASE_DIR . '/uploads/public/');
define('UPLOAD_DIR_PRIVATE', BASE_DIR . '/uploads/private/');

// Ensure Upload Directories Exist
if (!file_exists(UPLOAD_DIR_PUBLIC)) {
    @mkdir(UPLOAD_DIR_PUBLIC, 0755, true);
}
if (!file_exists(UPLOAD_DIR_PRIVATE)) {
    @mkdir(UPLOAD_DIR_PRIVATE, 0755, true);
    @file_put_contents(UPLOAD_DIR_PRIVATE . '.htaccess', "Order deny,allow\nDeny from all");
}

// Environment Overrides
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_NAME', getenv('DB_NAME') ?: 'doctorpoint_store');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') !== false ? getenv('DB_PASS') : '');
define('DB_PORT', getenv('DB_PORT') ?: '3306');
define('DB_CHARSET', 'utf8mb4');

// Firebase Authentication Configuration
define('FIREBASE_PROJECT_ID', getenv('FIREBASE_PROJECT_ID') ?: 'my-project-store-7047a');

// Enable Dev Token Bypass ONLY for local development on localhost/127.0.0.1
define('ALLOW_DEV_TOKENS', (
    getenv('ALLOW_DEV_TOKENS') === 'true' ||
    (isset($_SERVER['HTTP_HOST']) && in_array($_SERVER['HTTP_HOST'], ['localhost', '127.0.0.1', 'localhost:8000', '127.0.0.1:8000'], true))
));

// Configured Super Administrators (Emails that get automatic admin role sync)
$ADMIN_EMAILS = [
    'admin@novastore.com',
    'ravivadhiya46@gmail.com'
];

/**
 * Returns a Singleton PDO Database Connection
 *
 * @return PDO
 */
function getDbConnection() {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = sprintf(
            'mysql:host=%s;port=%s;dbname=%s;charset=%s',
            DB_HOST,
            DB_PORT,
            DB_NAME,
            DB_CHARSET
        );

        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci"
        ];

        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            error_log('[Database Error] Connection failed: ' . $e->getMessage());
            sendJsonResponse([
                'success' => false,
                'error'   => 'Database connection error. Please verify database configuration.'
            ], 500);
        }
    }
    return $pdo;
}

/**
 * Sends a standardized JSON Response and terminates execution
 *
 * @param array $data
 * @param int $statusCode
 * @return void
 */
function sendJsonResponse(array $data, int $statusCode = 200) {
    if (!headers_sent()) {
        http_response_code($statusCode);
        header('Content-Type: application/json; charset=utf-8');
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');
    }

    if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        exit(0);
    }

    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

/**
 * Sanitizes String Inputs
 *
 * @param string|null $input
 * @return string
 */
function sanitizeString($input) {
    if ($input === null) return '';
    return trim(htmlspecialchars(strip_tags((string)$input), ENT_QUOTES, 'UTF-8'));
}

/**
 * Helper to generate URL-friendly Slugs
 *
 * @param string $text
 * @return string
 */
function generateSlug(string $text): string {
    $slug = strtolower(trim($text));
    $slug = preg_replace('/[^a-z0-9]+/i', '-', $slug);
    $slug = trim($slug, '-');
    if (empty($slug)) {
        $slug = 'product';
    }
    return $slug . '-' . rand(1000, 9999);
}
