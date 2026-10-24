<?php
/**
 * Server-Side Firebase ID Token Verification Middleware
 * Designed for InfinityFree Shared Hosting & PHP 7.4+ / 8.x
 */

require_once __DIR__ . '/config.php';

/**
 * Extracts Authorization Bearer Token from HTTP Request Headers
 *
 * @return string|null
 */
function getBearerToken(): ?string {
    $headers = null;

    if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $headers = trim($_SERVER['HTTP_AUTHORIZATION']);
    } elseif (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $headers = trim($_SERVER['REDIRECT_HTTP_AUTHORIZATION']);
    } elseif (function_exists('apache_request_headers')) {
        $requestHeaders = apache_request_headers();
        $requestHeaders = array_combine(
            array_map('ucwords', array_keys($requestHeaders)),
            array_values($requestHeaders)
        );
        if (isset($requestHeaders['Authorization'])) {
            $headers = trim($requestHeaders['Authorization']);
        }
    }

    if (!empty($headers)) {
        if (preg_match('/Bearer\s(\S+)/i', $headers, $matches)) {
            return $matches[1];
        }
    }

    return null;
}

/**
 * Helper to decode Base64Url strings safely
 *
 * @param string $input
 * @return string
 */
function base64UrlDecode(string $input): string {
    $remainder = strlen($input) % 4;
    if ($remainder) {
        $padlen = 4 - $remainder;
        $input .= str_repeat('=', $padlen);
    }
    return base64_decode(strtr($input, '-_', '+/'));
}

/**
 * Cryptographically verifies Firebase ID token claims and signature
 *
 * @param string $token
 * @return array|null Decoded verified token claims (uid, email, name) or null if invalid
 */
function verifyFirebaseIdToken(string $token): ?array {
    $token = trim($token);
    if (empty($token)) {
        return null;
    }

    // Dev Token Fallback (Only active if ALLOW_DEV_TOKENS constant is explicitly true on localhost)
    if (defined('ALLOW_DEV_TOKENS') && ALLOW_DEV_TOKENS === true && strpos($token, 'dev-token-') === 0) {
        $parts = explode(':', $token);
        $email = isset($parts[1]) ? strtolower(trim($parts[1])) : 'user@example.com';
        $name  = isset($parts[2]) ? trim($parts[2]) : 'Dev User';
        $uid   = 'dev-uid-' . preg_replace('/[^a-z0-9]/', '', $email);

        return [
            'uid'   => $uid,
            'email' => $email,
            'name'  => $name
        ];
    }

    // Ensure token is formatted as a 3-part JWT
    $jwtParts = explode('.', $token);
    if (count($jwtParts) !== 3) {
        error_log("[Auth Error] Invalid JWT structure");
        return null;
    }

    list($headB64, $payloadB64, $sigB64) = $jwtParts;

    $header  = json_decode(base64UrlDecode($headB64), true);
    $payload = json_decode(base64UrlDecode($payloadB64), true);

    if (!$header || !$payload) {
        error_log("[Auth Error] Unable to decode JWT header or payload");
        return null;
    }

    // Check Expiration
    $now = time();
    $exp = (int)($payload['exp'] ?? 0);
    if ($exp <= 0 || $exp < ($now - 30)) { // Allow 30s clock skew
        error_log("[Auth Error] Firebase ID Token expired (exp: {$exp}, now: {$now})");
        return null;
    }

    // Check Project Audience & Issuer
    $expectedProjId = FIREBASE_PROJECT_ID;
    $aud = $payload['aud'] ?? '';
    $iss = $payload['iss'] ?? '';
    $expectedIss = 'https://securetoken.google.com/' . $expectedProjId;

    if (!empty($expectedProjId)) {
        if ($aud !== $expectedProjId) {
            error_log("[Auth Error] Token audience '{$aud}' does not match expected project ID '{$expectedProjId}'");
            return null;
        }
        if ($iss !== $expectedIss) {
            error_log("[Auth Error] Token issuer '{$iss}' does not match expected issuer '{$expectedIss}'");
            return null;
        }
    }

    $uid   = trim($payload['user_id'] ?? $payload['sub'] ?? '');
    $email = strtolower(trim($payload['email'] ?? ''));

    if (empty($uid)) {
        error_log("[Auth Error] Token payload missing user_id/sub claim");
        return null;
    }

    // Primary Verification: Google TokenInfo Verification Endpoint
    $verifyUrl = 'https://oauth2.googleapis.com/tokeninfo?id_token=' . urlencode($token);

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $verifyUrl);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 6);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode === 200 && !empty($response)) {
        $tokenInfo = json_decode($response, true);
        if ($tokenInfo && isset($tokenInfo['user_id']) && isset($tokenInfo['email'])) {
            $verifiedUid   = trim($tokenInfo['user_id']);
            $verifiedEmail = strtolower(trim($tokenInfo['email']));

            if ($verifiedUid === $uid && $verifiedEmail === $email) {
                return [
                    'uid'   => $uid,
                    'email' => $email,
                    'name'  => !empty($payload['name']) ? $payload['name'] : (!empty($tokenInfo['name']) ? $tokenInfo['name'] : $email)
                ];
            }
        }
    }

    // Fallback Verification: RS256 Signature verification using Google x509 public certificates
    $kid = $header['kid'] ?? null;
    $alg = $header['alg'] ?? null;

    if ($alg === 'RS256' && !empty($kid) && function_exists('openssl_verify')) {
        $keysUrl = 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com';
        $keysJson = @file_get_contents($keysUrl);
        if ($keysJson) {
            $certs = json_decode($keysJson, true);
            if (isset($certs[$kid])) {
                $pubKey = $certs[$kid];
                $dataToVerify = $headB64 . '.' . $payloadB64;
                $signature = base64UrlDecode($sigB64);

                $verified = openssl_verify($dataToVerify, $signature, $pubKey, OPENSSL_ALGO_SHA256);
                if ($verified === 1) {
                    return [
                        'uid'   => $uid,
                        'email' => $email,
                        'name'  => !empty($payload['name']) ? $payload['name'] : $email
                    ];
                }
            }
        }
    }

    error_log("[Auth Error] Firebase ID token cryptographic verification failed");
    return null;
}

/**
 * Authenticates request, verifies token claims, and connects user to MySQL database
 *
 * @param bool $required If true, terminates execution with 401 on failure
 * @return array|null MySQL user record or null
 */
function authenticateUser(bool $required = true): ?array {
    global $ADMIN_EMAILS;

    $token = getBearerToken();

    if (!$token) {
        if ($required) {
            sendJsonResponse([
                'success' => false,
                'error'   => 'Authentication required. Missing Bearer authorization header.'
            ], 401);
        }
        return null;
    }

    $verified = verifyFirebaseIdToken($token);

    if (!$verified) {
        if ($required) {
            sendJsonResponse([
                'success' => false,
                'error'   => 'Invalid, expired, or unverified authentication token.'
            ], 401);
        }
        return null;
    }

    $db = getDbConnection();

    // Query MySQL users table by verified Firebase UID
    $stmt = $db->prepare("SELECT * FROM users WHERE firebase_uid = :uid LIMIT 1");
    $stmt->execute([':uid' => $verified['uid']]);
    $user = $stmt->fetch();

    $email = strtolower($verified['email']);
    $isAdminConfigured = in_array($email, array_map('strtolower', $ADMIN_EMAILS), true);

    if (!$user) {
        // Auto-provision new user record in MySQL using verified server claims ONLY
        $targetRole = $isAdminConfigured ? 'admin' : 'user';

        $insertStmt = $db->prepare("
            INSERT INTO users (firebase_uid, email, display_name, role)
            VALUES (:uid, :email, :name, :role)
        ");
        $insertStmt->execute([
            ':uid'   => $verified['uid'],
            ':email' => $email,
            ':name'  => $verified['name'],
            ':role'  => $targetRole
        ]);

        $newId = $db->lastInsertId();
        $user = [
            'id'           => $newId,
            'firebase_uid' => $verified['uid'],
            'email'        => $email,
            'display_name' => $verified['name'],
            'role'         => $targetRole
        ];
    } else {
        // Automatically sync admin role if configured super administrator
        if ($isAdminConfigured && $user['role'] !== 'admin') {
            $updateStmt = $db->prepare("UPDATE users SET role = 'admin' WHERE id = :id");
            $updateStmt->execute([':id' => $user['id']]);
            $user['role'] = 'admin';
        }
    }

    $user['is_admin'] = ($user['role'] === 'admin');
    return $user;
}

/**
 * Enforces Administrator access control
 *
 * @return array Admin user record from MySQL
 */
function requireAdmin(): array {
    $user = authenticateUser(true);

    if (!$user || $user['role'] !== 'admin') {
        sendJsonResponse([
            'success' => false,
            'error'   => 'Access denied. Administrator privileges required.'
        ], 403);
    }

    return $user;
}
