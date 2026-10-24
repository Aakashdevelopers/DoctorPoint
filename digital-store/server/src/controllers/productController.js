const Product = require('../models/Product');
const Purchase = require('../models/Purchase');
const Category = require('../models/Category');
const StorageService = require('../services/storageService');
const path = require('path');

const parseFeatures = (featuresInput) => {
  if (!featuresInput) return [];
  if (Array.isArray(featuresInput)) return featuresInput.filter(f => f.trim());
  if (typeof featuresInput === 'string') {
    return featuresInput
      .split(/\r?\n|,/)
      .map(f => f.trim())
      .filter(f => f.length > 0);
  }
  return [];
};

const parseTestimonials = (testimonialsInput) => {
  if (!testimonialsInput) return [];
  if (Array.isArray(testimonialsInput)) return testimonialsInput;
  if (typeof testimonialsInput === 'string') {
    try {
      const parsed = JSON.parse(testimonialsInput);
      if (Array.isArray(parsed)) return parsed;
    } catch (e) {}
  }
  return [];
};

const parseFaqs = (faqsInput) => {
  if (!faqsInput) return [];
  if (Array.isArray(faqsInput)) return faqsInput;
  if (typeof faqsInput === 'string') {
    try {
      const parsed = JSON.parse(faqsInput);
      if (Array.isArray(parsed)) return parsed;
    } catch (e) {}
  }
  return [];
};

/**
 * Public: Get Published Products for Storefront
 */
exports.getPublishedProducts = async (req, res, next) => {
  try {
    const { category, search } = req.query;
    const query = { status: 'published' };

    if (category && category !== 'All') {
      query.category = { $regex: new RegExp(`^${category}$`, 'i') };
    }

    if (search) {
      query.$or = [
        { title: { $regex: search, $options: 'i' } },
        { shortDescription: { $regex: search, $options: 'i' } },
        { category: { $regex: search, $options: 'i' } }
      ];
    }

    const products = await Product.find(query).sort({ createdAt: -1 });

    const formatted = products.map(p => ({
      _id: p._id,
      title: p.title,
      slug: p.slug,
      shortDescription: p.shortDescription,
      coverImageUrl: StorageService.getPublicImageUrl(p.coverImagePath, req),
      priceInINR: (p.priceInPaise / 100).toFixed(0),
      compareAtPriceInINR: p.compareAtPriceInPaise ? (p.compareAtPriceInPaise / 100).toFixed(0) : null,
      category: p.category,
      createdAt: p.createdAt
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
 * Public: Get Categories (Combines custom admin categories & published product categories)
 */
exports.getCategories = async (req, res, next) => {
  try {
    const customCategories = await Category.find().select('name');
    const customNames = customCategories.map(c => c.name);
    const prodCategories = await Product.distinct('category', { status: 'published' });

    const combinedSet = new Set([...customNames, ...prodCategories]);
    const finalCategories = ['All', ...Array.from(combinedSet).filter(c => c && c.trim())];

    res.json({
      success: true,
      data: finalCategories
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Public: Get Single Product Details by Slug
 */
exports.getProductBySlug = async (req, res, next) => {
  try {
    const { slug } = req.params;
    const product = await Product.findOne({ slug });

    if (!product) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    // Only admins can view drafts or archived products
    if (product.status !== 'published') {
      const isUserAdmin = req.user && req.user.admin;
      if (!isUserAdmin) {
        return res.status(404).json({ success: false, error: 'Product is currently unavailable' });
      }
    }

    let isPurchased = false;
    let purchaseId = null;

    if (req.user && req.user.uid) {
      const existingPurchase = await Purchase.findOne({
        userId: req.user.uid,
        productId: product._id
      });
      if (existingPurchase) {
        isPurchased = true;
        purchaseId = existingPurchase._id;
      }
    }

    const fileSizeFormatted = product.fileSize
      ? (product.fileSize > 1024 * 1024
          ? `${(product.fileSize / (1024 * 1024)).toFixed(2)} MB`
          : `${(product.fileSize / 1024).toFixed(2)} KB`)
      : 'Digital Download';

    res.json({
      success: true,
      data: {
        _id: product._id,
        title: product.title,
        slug: product.slug,
        shortDescription: product.shortDescription,
        description: product.description,
        coverImageUrl: StorageService.getPublicImageUrl(product.coverImagePath, req),
        priceInINR: (product.priceInPaise / 100).toFixed(0),
        compareAtPriceInINR: product.compareAtPriceInPaise ? (product.compareAtPriceInPaise / 100).toFixed(0) : null,
        category: product.category,
        fileName: product.fileName,
        fileSizeFormatted,
        resourceUrl: product.resourceUrl || 'https://drive.google.com/',
        supportEmail: product.supportEmail || 'support@novastore.com',
        supportPhone: product.supportPhone || '+91 9876543210',
        features: product.features && product.features.length ? product.features : [],
        testimonials: product.testimonials || [],
        faqs: product.faqs || [],
        resourceCount: product.resourceCount || 1,
        resourceType: product.resourceType || 'File',
        status: product.status,
        isPurchased,
        purchaseId
      }
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Get All Products (Drafts, Published, Archived)
 */
exports.getAllProductsAdmin = async (req, res, next) => {
  try {
    const { search, status } = req.query;
    const query = {};

    if (status && status !== 'all') {
      query.status = status;
    }

    if (search) {
      query.$or = [
        { title: { $regex: search, $options: 'i' } },
        { category: { $regex: search, $options: 'i' } }
      ];
    }

    const products = await Product.find(query).sort({ createdAt: -1 });

    const formatted = products.map(p => ({
      _id: p._id,
      title: p.title,
      slug: p.slug,
      shortDescription: p.shortDescription,
      description: p.description,
      coverImageUrl: StorageService.getPublicImageUrl(p.coverImagePath, req),
      priceInINR: (p.priceInPaise / 100).toFixed(0),
      compareAtPriceInINR: p.compareAtPriceInPaise ? (p.compareAtPriceInPaise / 100).toFixed(0) : null,
      category: p.category,
      resourceUrl: p.resourceUrl || 'https://drive.google.com/',
      supportEmail: p.supportEmail || 'support@novastore.com',
      supportPhone: p.supportPhone || '+91 9876543210',
      features: p.features || [],
      testimonials: p.testimonials || [],
      faqs: p.faqs || [],
      resourceCount: p.resourceCount || 1,
      resourceType: p.resourceType || 'File',
      status: p.status,
      createdAt: p.createdAt,
      archivedAt: p.archivedAt
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
 * Admin: Create Product
 */
exports.createProduct = async (req, res, next) => {
  try {
    const {
      title,
      shortDescription,
      description,
      priceInINR,
      compareAtPriceInINR,
      category,
      resourceUrl,
      supportEmail,
      supportPhone,
      features,
      testimonials,
      faqs,
      resourceCount,
      resourceType,
      status
    } = req.body;

    if (!req.files || !req.files.coverImage || !req.files.digitalFile) {
      return res.status(400).json({
        success: false,
        error: 'Both Cover Image and Digital Product File are required.'
      });
    }

    const coverFile = req.files.coverImage[0];
    const digitalFile = req.files.digitalFile[0];

    const priceInPaise = Math.round(parseFloat(priceInINR || 0) * 100);
    const compareAtPriceInPaise = compareAtPriceInINR ? Math.round(parseFloat(compareAtPriceInINR) * 100) : 0;

    const product = await Product.create({
      title,
      shortDescription,
      description,
      coverImagePath: coverFile.filename,
      privateFilePath: digitalFile.filename,
      fileName: digitalFile.originalname,
      fileSize: digitalFile.size,
      priceInPaise,
      compareAtPriceInPaise,
      category: category || 'General',
      resourceUrl: resourceUrl || 'https://drive.google.com/',
      supportEmail: supportEmail || 'support@novastore.com',
      supportPhone: supportPhone || '+91 9574055968',
      features: parseFeatures(features),
      testimonials: parseTestimonials(testimonials),
      faqs: parseFaqs(faqs),
      resourceCount: resourceCount ? parseInt(resourceCount) : 1,
      resourceType: resourceType || 'File',
      status: status || 'draft'
    });

    res.status(201).json({
      success: true,
      message: 'Product created successfully',
      data: product
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Update Product
 */
exports.updateProduct = async (req, res, next) => {
  try {
    const { id } = req.params;
    const {
      title,
      shortDescription,
      description,
      priceInINR,
      compareAtPriceInINR,
      category,
      resourceUrl,
      supportEmail,
      supportPhone,
      features,
      testimonials,
      faqs,
      resourceCount,
      resourceType,
      status
    } = req.body;

    const product = await Product.findById(id);
    if (!product) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    if (title) product.title = title;
    if (shortDescription) product.shortDescription = shortDescription;
    if (description) product.description = description;
    if (category) product.category = category;
    if (resourceUrl) product.resourceUrl = resourceUrl;
    if (supportEmail) product.supportEmail = supportEmail;
    if (supportPhone) product.supportPhone = supportPhone;
    if (resourceCount !== undefined) product.resourceCount = parseInt(resourceCount) || 1;
    if (resourceType) product.resourceType = resourceType;
    if (features !== undefined) product.features = parseFeatures(features);
    if (testimonials !== undefined) product.testimonials = parseTestimonials(testimonials);
    if (faqs !== undefined) product.faqs = parseFaqs(faqs);
    if (status) product.status = status;
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    if (title) product.title = title;
    if (shortDescription) product.shortDescription = shortDescription;
    if (description) product.description = description;
    if (category) product.category = category;
    if (resourceUrl) product.resourceUrl = resourceUrl;
    if (supportEmail) product.supportEmail = supportEmail;
    if (supportPhone) product.supportPhone = supportPhone;
    if (resourceCount !== undefined) product.resourceCount = parseInt(resourceCount) || 1;
    if (resourceType) product.resourceType = resourceType;
    if (features !== undefined) product.features = parseFeatures(features);
    if (testimonials !== undefined) product.testimonials = parseTestimonials(testimonials);
    if (status) product.status = status;

    if (priceInINR !== undefined) {
      product.priceInPaise = Math.round(parseFloat(priceInINR) * 100);
    }
    if (compareAtPriceInINR !== undefined) {
      product.compareAtPriceInPaise = compareAtPriceInINR ? Math.round(parseFloat(compareAtPriceInINR) * 100) : 0;
    }

    if (req.files) {
      if (req.files.coverImage && req.files.coverImage[0]) {
        product.coverImagePath = req.files.coverImage[0].filename;
      }
      if (req.files.digitalFile && req.files.digitalFile[0]) {
        product.privateFilePath = req.files.digitalFile[0].filename;
        product.fileName = req.files.digitalFile[0].originalname;
        product.fileSize = req.files.digitalFile[0].size;
      }
    }

    await product.save();

    res.json({
      success: true,
      message: 'Product updated successfully',
      data: product
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Change Product Status (publish / unpublish / archive)
 */
exports.updateProductStatus = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    if (!['draft', 'published', 'archived'].includes(status)) {
      return res.status(400).json({ success: false, error: 'Invalid status value' });
    }

    const product = await Product.findById(id);
    if (!product) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    product.status = status;
    if (status === 'archived') {
      product.archivedAt = new Date();
    } else {
      product.archivedAt = null;
    }

    await product.save();

    res.json({
      success: true,
      message: `Product status updated to ${status}`,
      data: product
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Admin: Archive Product safely preserving purchase history
 */
exports.archiveProduct = async (req, res, next) => {
  try {
    const { id } = req.params;
    const product = await Product.findById(id);

    if (!product) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    product.status = 'archived';
    product.archivedAt = new Date();
    await product.save();

    res.json({
      success: true,
      message: 'Product safely archived. Historical customer purchase records preserved.'
    });
  } catch (err) {
    next(err);
  }
};
