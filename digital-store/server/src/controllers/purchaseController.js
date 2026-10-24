const Purchase = require('../models/Purchase');
const Product = require('../models/Product');
const StorageService = require('../services/storageService');
const fs = require('fs');

/**
 * Instant Demo Buy Controller
 * Atomically grants purchase access without payment gateway call
 */
exports.processDemoBuy = async (req, res, next) => {
  try {
    const { productId, phoneNumber, customerEmail } = req.body;
    const userId = req.user.uid;
    const userEmail = customerEmail || req.user.email;

    if (!productId) {
      return res.status(400).json({ success: false, error: 'Product ID is required.' });
    }

    // Server-authoritative check: Fetch product directly from DB
    const product = await Product.findById(productId);

    if (!product) {
      return res.status(404).json({ success: false, error: 'Product not found.' });
    }

    if (product.status !== 'published') {
      return res.status(400).json({
        success: false,
        error: 'This product is unavailable or not currently published for purchase.'
      });
    }

    // Check if user already owns this product
    let purchase = await Purchase.findOne({ userId, productId });

    if (purchase) {
      return res.status(200).json({
        success: true,
        alreadyOwned: true,
        message: 'Product already present in your purchases library.',
        data: {
          purchaseId: purchase._id,
          purchaseType: purchase.purchaseType,
          status: purchase.status,
          resourceUrl: product.resourceUrl || 'https://drive.google.com/',
          createdAt: purchase.createdAt
        }
      });
    }

    // Create new demo purchase record
    try {
      purchase = await Purchase.create({
        userId,
        userEmail,
        phoneNumber: phoneNumber || '',
        productId,
        purchaseType: 'demo',
        status: 'granted',
        priceSnapshotInPaise: product.priceInPaise
      });
    } catch (err) {
      // Handle rare race-condition duplicate key error
      if (err.code === 11000) {
        purchase = await Purchase.findOne({ userId, productId });
        return res.status(200).json({
          success: true,
          alreadyOwned: true,
          message: 'Product already present in your purchases library.',
          data: {
            purchaseId: purchase._id,
            purchaseType: purchase.purchaseType,
            status: purchase.status,
            resourceUrl: product.resourceUrl || 'https://drive.google.com/',
            createdAt: purchase.createdAt
          }
        });
      }
      throw err;
    }

    res.status(201).json({
      success: true,
      alreadyOwned: false,
      message: 'Demo purchase granted successfully!',
      data: {
        purchaseId: purchase._id,
        productId: product._id,
        productTitle: product.title,
        resourceUrl: product.resourceUrl || 'https://drive.google.com/',
        purchaseType: 'demo',
        status: 'granted',
        createdAt: purchase.createdAt,
        demoNotice: 'Demo Purchase — No Payment Processed'
      }
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get My Purchases List
 */
exports.getMyPurchases = async (req, res, next) => {
  try {
    const userId = req.user.uid;

    const purchases = await Purchase.find({ userId })
      .populate('productId')
      .sort({ createdAt: -1 });

    const formatted = purchases
      .filter(p => p.productId) // Filter out deleted products if any
      .map(p => {
        const prod = p.productId;
        return {
          purchaseId: p._id,
          purchaseType: p.purchaseType,
          status: p.status,
          purchaseDate: p.createdAt,
          pricePaidINR: (p.priceSnapshotInPaise / 100).toFixed(0),
          demoNotice: 'Demo Purchase — No Payment Processed',
          product: {
            _id: prod._id,
            title: prod.title,
            slug: prod.slug,
            shortDescription: prod.shortDescription,
            category: prod.category,
            fileName: prod.fileName,
            fileSizeFormatted: prod.fileSize ? `${(prod.fileSize / (1024 * 1024)).toFixed(2)} MB` : 'Digital Download',
            resourceUrl: prod.resourceUrl || 'https://drive.google.com/',
            coverImageUrl: StorageService.getPublicImageUrl(prod.coverImagePath, req)
          }
        };
      });

    res.json({
      success: true,
      count: formatted.length,
      data: formatted
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Authorized File Download
 * Verifies purchase ownership before serving private digital file
 */
exports.downloadPurchasedFile = async (req, res, next) => {
  try {
    const { purchaseId } = req.params;
    const userId = req.user.uid;

    const purchase = await Purchase.findById(purchaseId).populate('productId');

    if (!purchase) {
      return res.status(404).json({ success: false, error: 'Purchase record not found.' });
    }

    // Strict Authorization: Verify file belongs to authenticated user
    if (purchase.userId !== userId && !req.user.admin) {
      return res.status(403).json({
        success: false,
        error: 'Forbidden. You do not have permission to access or download this file.'
      });
    }

    const product = purchase.productId;
    if (!product || !product.privateFilePath) {
      return res.status(404).json({
        success: false,
        error: 'Digital file not found for this product.'
      });
    }

    const fullFilePath = StorageService.getPrivateFilePath(product.privateFilePath);

    if (!fullFilePath) {
      // Dev mode fallback file generator if physical upload file is missing locally
      res.setHeader('Content-Type', 'text/plain');
      res.setHeader('Content-Disposition', `attachment; filename="${product.fileName || 'digital-product.txt'}"`);
      return res.send(`NovaStore Authorized Digital Product File\n\nTitle: ${product.title}\nCategory: ${product.category}\nResource URL: ${product.resourceUrl || 'https://drive.google.com/'}\nOwner UID: ${userId}\nPurchase ID: ${purchase._id}\nType: Demo Grant\nStatus: Active`);
    }

    const downloadFileName = product.fileName || 'digital-download';
    res.download(fullFilePath, downloadFileName, (err) => {
      if (err && !res.headersSent) {
        console.error(`[Download Controller] Download stream error: ${err.message}`);
        res.status(500).json({ success: false, error: 'Error streaming file download.' });
      }
    });
  } catch (err) {
    next(err);
  }
};
