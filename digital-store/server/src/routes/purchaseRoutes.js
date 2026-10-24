const express = require('express');
const router = express.Router();
const purchaseController = require('../controllers/purchaseController');
const { verifyAuth } = require('../middleware/auth');

// Protected User Routes (Require Authentication)
router.post('/demo-buy', verifyAuth, purchaseController.processDemoBuy);
router.get('/my-purchases', verifyAuth, purchaseController.getMyPurchases);
router.get('/:purchaseId/download', verifyAuth, purchaseController.downloadPurchasedFile);

module.exports = router;
