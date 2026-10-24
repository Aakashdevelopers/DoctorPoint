const express = require('express');
const path = require('path');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const errorHandler = require('./middleware/errorHandler');

const productRoutes = require('./routes/productRoutes');
const purchaseRoutes = require('./routes/purchaseRoutes');
const adminRoutes = require('./routes/adminRoutes');
const authRoutes = require('./routes/authRoutes');

const app = express();

// Security Headers
app.use(
  helmet({
    contentSecurityPolicy: false,
    crossOriginResourcePolicy: { policy: 'cross-origin' }
  })
);

// CORS
app.use(cors());

// Body Parsers
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Rate Limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000'),
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '200'),
  message: { success: false, error: 'Too many requests from this IP. Please try again later.' }
});
app.use('/api/', limiter);

// Serve Static Public Files
const publicUploadsDir = path.resolve(__dirname, '../uploads/public');
app.use('/uploads/public', express.static(publicUploadsDir));

// Serve Admin Panel Static Files (from digital-store/admin/)
const adminDir = path.resolve(__dirname, '../../admin');
app.use('/admin', express.static(adminDir));

// Serve User Panel Static Files (from digital-store/user/)
const userDir = path.resolve(__dirname, '../../user');
app.use(express.static(userDir));

// API Routes
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    store: 'NovaStore API',
    environment: process.env.NODE_ENV || 'development',
    timestamp: new Date()
  });
});

app.use('/api/products', productRoutes);
app.use('/api/purchases', purchaseRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/auth', authRoutes);

// Fallback for SPA routing
app.get('*', (req, res) => {
  if (req.path.startsWith('/api/')) {
    return res.status(404).json({ success: false, error: 'API route not found' });
  }
  if (req.path.startsWith('/admin')) {
    return res.sendFile(path.join(adminDir, 'index.html'));
  }
  res.sendFile(path.join(userDir, 'index.html'));
});

// Centralized Error Handler
app.use(errorHandler);

module.exports = app;
