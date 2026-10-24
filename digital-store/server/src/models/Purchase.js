const mongoose = require('mongoose');

const purchaseSchema = new mongoose.Schema(
  {
    userId: {
      type: String, // Stores the firebaseUid
      required: true,
      index: true
    },
    userEmail: {
      type: String,
      lowercase: true,
      trim: true
    },
    phoneNumber: {
      type: String,
      trim: true,
      default: ''
    },
    productId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Product',
      required: true,
      index: true
    },
    purchaseType: {
      type: String,
      enum: ['demo'],
      default: 'demo',
      required: true
    },
    status: {
      type: String,
      enum: ['granted'],
      default: 'granted',
      required: true
    },
    priceSnapshotInPaise: {
      type: Number,
      required: true,
      default: 0
    }
  },
  {
    timestamps: true
  }
);

purchaseSchema.index({ userId: 1, productId: 1 }, { unique: true });

module.exports = mongoose.model('Purchase', purchaseSchema);
