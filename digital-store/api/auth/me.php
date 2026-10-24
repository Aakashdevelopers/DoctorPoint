<?php
/**
 * Auth Me Endpoint - Returns current authenticated user profile
 * Endpoint: GET /api/auth/me
 */

require_once __DIR__ . '/../../config/auth.php';

$user = authenticateUser(true);

sendJsonResponse([
    'success' => true,
    'data'    => [
        'id'          => $user['id'],
        'uid'         => $user['firebase_uid'],
        'email'       => $user['email'],
        'displayName' => $user['display_name'],
        'role'        => $user['role'],
        'isAdmin'     => $user['is_admin']
    ]
]);
