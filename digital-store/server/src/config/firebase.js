const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

let isDevFallback = false;

function initFirebase() {
  if (admin.apps.length > 0) {
    return admin;
  }

  try {
    // Check Option A: Service account JSON string in ENV
    if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        storageBucket: process.env.FIREBASE_STORAGE_BUCKET
      });
      console.log('[Firebase Admin] Initialized via FIREBASE_SERVICE_ACCOUNT_JSON');
      return admin;
    }

    // Check Option B: Service account JSON file path
    const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || path.join(__dirname, 'firebase-service-account.json');
    if (fs.existsSync(serviceAccountPath)) {
      const serviceAccount = require(path.resolve(serviceAccountPath));
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        storageBucket: process.env.FIREBASE_STORAGE_BUCKET
      });
      console.log(`[Firebase Admin] Initialized via file: ${serviceAccountPath}`);
      return admin;
    }

    // Check Option C: GOOGLE_APPLICATION_CREDENTIALS
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        storageBucket: process.env.FIREBASE_STORAGE_BUCKET
      });
      console.log('[Firebase Admin] Initialized via applicationDefault()');
      return admin;
    }

    console.warn('[Firebase Admin] No service account key found. Enabling Development Auth Fallback mode.');
    isDevFallback = true;
    return null;
  } catch (err) {
    console.error(`[Firebase Admin] Initialization error: ${err.message}`);
    isDevFallback = true;
    return null;
  }
}

initFirebase();

module.exports = {
  admin,
  isDevFallback: () => isDevFallback
};
