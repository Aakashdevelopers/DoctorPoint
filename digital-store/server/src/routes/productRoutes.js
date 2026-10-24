const express = require('express');
const router = express.Router();
const productController = require('../controllers/productController');
const { verifyAuth } = require('../middleware/auth');

// Public storefront routes
router.get('/', productController.getPublishedProducts);
router.get('/categories', productController.getCategories);

// Optional auth route handler: checks token if present to return ownership status
router.get('/:slug', (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return verifyAuth(req, res, () => productController.getProductBySlug(req, res, next));
  }
  return productController.getProductBySlug(req, res, next);
});

module.exports = router;
