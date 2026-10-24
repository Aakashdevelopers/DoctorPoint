const mongoose = require('mongoose');
const slugify = require('slugify');

const productSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: [true, 'Product title is required'],
      trim: true,
      maxlength: [120, 'Title cannot exceed 120 characters']
    },
    slug: {
      type: String,
      unique: true,
      index: true
    },
    shortDescription: {
      type: String,
      required: [true, 'Short description is required'],
      trim: true,
      maxlength: [250, 'Short description cannot exceed 250 characters']
    },
    description: {
      type: String,
      required: [true, 'Full description is required'],
      trim: true
    },
    coverImagePath: {
      type: String,
      required: [true, 'Cover image is required']
    },
    privateFilePath: {
      type: String,
      required: [true, 'Private digital file path is required']
    },
    resourceUrl: {
      type: String,
      trim: true,
      default: 'https://drive.google.com/'
    },
    fileName: {
      type: String,
      default: 'digital-product-file'
    },
    fileSize: {
      type: Number,
      default: 0
    },
    priceInPaise: {
      type: Number,
      required: [true, 'Price is required'],
      min: [0, 'Price cannot be negative']
    },
    compareAtPriceInPaise: {
      type: Number,
      min: [0, 'Compare price cannot be negative'],
      default: 0
    },
    category: {
      type: String,
      required: [true, 'Category is required'],
      trim: true,
      index: true
    },
    supportEmail: {
      type: String,
      trim: true,
      default: 'support@novastore.com'
    },
    supportPhone: {
      type: String,
      trim: true,
      default: '+91 9876543210'
    },
    features: {
      type: [String],
      default: []
    },
    resourceCount: {
      type: Number,
      default: 1
    },
    resourceType: {
      type: String,
      default: 'File'
    },
    testimonials: [
      {
        authorName: { type: String, required: true },
        authorRole: { type: String, default: '' },
        rating: { type: Number, default: 5, min: 1, max: 5 },
        comment: { type: String, required: true },
        avatarUrl: { type: String, default: '' }
      }
    ],
    faqs: [
      {
        question: { type: String, required: true },
        answer: { type: String, required: true }
      }
    ],
    status: {
      type: String,
      enum: ['draft', 'published', 'archived'],
      default: 'draft',
      index: true
    },
    archivedAt: {
      type: Date,
      default: null
    }
  },
  {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
  }
);

// Auto-generate slug before validation
productSchema.pre('validate', function (next) {
  if (this.title && (!this.slug || this.isModified('title'))) {
    const baseSlug = slugify(this.title, { lower: true, strict: true });
    this.slug = `${baseSlug}-${Math.floor(1000 + Math.random() * 9000)}`;
  }
  next();
});

// Virtual helper for Price in INR
productSchema.virtual('priceInINR').get(function () {
  return (this.priceInPaise / 100).toFixed(0);
});

productSchema.virtual('compareAtPriceInINR').get(function () {
  return this.compareAtPriceInPaise ? (this.compareAtPriceInPaise / 100).toFixed(0) : null;
});

module.exports = mongoose.model('Product', productSchema);
