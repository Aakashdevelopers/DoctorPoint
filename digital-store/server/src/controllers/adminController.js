const Product = require('../models/Product');
const Purchase = require('../models/Purchase');
const Category = require('../models/Category');

/**
 * Admin Dashboard Metrics & Statistics
 */
exports.getAdminStats = async (req, res, next) => {
  try {
    const totalProducts = await Product.countDocuments();
    const publishedProducts = await Product.countDocuments({ status: 'published' });
    const draftProducts = await Product.countDocuments({ status: 'draft' });
    const archivedProducts = await Product.countDocuments({ status: 'archived' });
    const totalDemoPurchases = await Purchase.countDocuments({ purchaseType: 'demo' });

    const recentPurchases = await Purchase.find()
      .populate('productId', 'title slug')
      .sort({ createdAt: -1 })
      .limit(5);

    const recentActivity = recentPurchases.map(p => ({
      purchaseId: p._id,
      userEmail: p.userEmail || p.userId,
      productTitle: p.productId ? p.productId.title : 'Archived Product',
      purchaseType: p.purchaseType,
      status: p.status,
      date: p.createdAt,
      demoLabel: 'Demo Purchase — No Payment Processed'
    }));

    res.json({
      success: true,
      data: {
        totalProducts,
        publishedProducts,
        draftProducts,
        archivedProducts,
        totalDemoPurchases,
        recentActivity
      }
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin Demo Purchases Audit Trail
 */
exports.getAdminPurchases = async (req, res, next) => {
  try {
    const purchases = await Purchase.find()
      .populate('productId', 'title category priceInPaise')
      .sort({ createdAt: -1 });

    const formatted = purchases.map(p => ({
      purchaseId: p._id,
      userId: p.userId,
      userEmail: p.userEmail || 'N/A',
      productTitle: p.productId ? p.productId.title : 'Archived Product',
      category: p.productId ? p.productId.category : 'N/A',
      purchaseType: 'demo',
      status: p.status,
      createdAt: p.createdAt,
      demoLabel: 'Demo Purchase — No Payment Processed'
    }));

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
 * Admin: Get Custom Categories List
 */
exports.getCategoriesAdmin = async (req, res, next) => {
  try {
    const categories = await Category.find().sort({ name: 1 });
    res.json({
      success: true,
      data: categories
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Create New Category
 */
exports.createCategoryAdmin = async (req, res, next) => {
  try {
    const { name } = req.body;
    if (!name || !name.trim()) {
      return res.status(400).json({ success: false, error: 'Category name is required.' });
    }

    const trimmedName = name.trim();
    const slug = trimmedName.toLowerCase().replace(/[^a-z0-9]+/g, '-');

    const existing = await Category.findOne({ name: { $regex: new RegExp(`^${trimmedName}$`, 'i') } });
    if (existing) {
      return res.status(400).json({ success: false, error: 'Category with this name already exists.' });
    }

    const category = await Category.create({ name: trimmedName, slug });

    res.status(201).json({
      success: true,
      message: 'Category created successfully',
      data: category
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Delete Category
 */
exports.deleteCategoryAdmin = async (req, res, next) => {
  try {
    const { id } = req.params;
    await Category.findByIdAndDelete(id);
    res.json({
      success: true,
      message: 'Category deleted successfully.'
    });
  } catch (err) {
    next(err);
  }
};
