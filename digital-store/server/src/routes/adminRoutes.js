const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const productController = require('../controllers/productController');
const { verifyAuth, requireAdmin } = require('../middleware/auth');
const { productUploads } = require('../middleware/upload');

// All Admin Routes require Authentication and Admin Privileges
router.use(verifyAuth, requireAdmin);

// Dashboard & Purchase Auditing
router.get('/stats', adminController.getAdminStats);
router.get('/purchases', adminController.getAdminPurchases);

// Category Management
router.get('/categories', adminController.getCategoriesAdmin);
router.post('/categories', adminController.createCategoryAdmin);
router.delete('/categories/:id', adminController.deleteCategoryAdmin);

// Product Management
router.get('/products', productController.getAllProductsAdmin);
router.post('/products', productUploads, productController.createProduct);
router.put('/products/:id', productUploads, productController.updateProduct);
router.patch('/products/:id/status', productController.updateProductStatus);
router.delete('/products/:id', productController.archiveProduct);

module.exports = router;
