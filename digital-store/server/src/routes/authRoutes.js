const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { verifyAuth } = require('../middleware/auth');

router.post('/sync', verifyAuth, authController.syncUserProfile);
router.get('/me', verifyAuth, authController.getCurrentUser);

module.exports = router;
