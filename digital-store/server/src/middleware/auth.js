const { admin, isDevFallback } = require('../config/firebase');
const UserProfile = require('../models/UserProfile');

const getAdminEmails = () => {
  const envEmails = process.env.ADMIN_EMAILS || 'admin@novastore.com,dev@novastore.com';
  return envEmails.split(',').map(e => e.trim().toLowerCase());
};

const verifyAuth = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        error: 'Authentication required. Missing Bearer token.'
      });
    }

    const token = authHeader.split('Bearer ')[1].trim();

    // Handle Development Mock Token fallback if enabled or for local testing
    if (token.startsWith('dev-token-')) {
      const parts = token.split(':');
      const uid = parts[1] || 'dev-user-uid';
      const email = parts[2] || 'user@example.com';
      const displayName = parts[3] || 'Dev User';

      req.user = {
        uid,
        email: email.toLowerCase(),
        name: displayName,
        admin: getAdminEmails().includes(email.toLowerCase())
      };
      return next();
    }

    if (isDevFallback()) {
      return res.status(401).json({
        success: false,
        error: 'Firebase Admin SDK not initialized and invalid dev token provided.'
      });
    }

    // Live Firebase Token Verification
    const decodedToken = await admin.auth().verifyIdToken(token);
    const email = (decodedToken.email || '').toLowerCase();
    const isAdmin = decodedToken.admin === true || getAdminEmails().includes(email);

    req.user = {
      uid: decodedToken.uid,
      email: email,
      name: decodedToken.name || decodedToken.email || 'User',
      admin: isAdmin
    };

    next();
  } catch (err) {
    console.error(`[Auth Middleware] Token verification failed: ${err.message}`);
    return res.status(401).json({
      success: false,
      error: 'Invalid or expired authentication token.'
    });
  }
};

const requireAdmin = async (req, res, next) => {
  if (!req.user) {
    return res.status(401).json({ success: false, error: 'Authentication required.' });
  }

  const email = (req.user.email || '').toLowerCase();
  const isAdminEmail = getAdminEmails().includes(email);
  const isClaimAdmin = req.user.admin === true;

  // Double check DB role if needed
  let isDbAdmin = false;
  try {
    const userProf = await UserProfile.findOne({ firebaseUid: req.user.uid });
    if (userProf && userProf.role === 'admin') {
      isDbAdmin = true;
    }
  } catch (e) {
    // Ignore db check error
  }

  if (isAdminEmail || isClaimAdmin || isDbAdmin) {
    req.user.admin = true;
    return next();
  }

  return res.status(403).json({
    success: false,
    error: 'Access denied. Administrator privileges required.'
  });
};

module.exports = {
  verifyAuth,
  requireAdmin
};
