const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Ensure upload target directories exist
const publicDir = path.resolve(__dirname, '../../uploads/public');
const privateDir = path.resolve(__dirname, '../../uploads/private');

if (!fs.existsSync(publicDir)) {
  fs.mkdirSync(publicDir, { recursive: true });
}
if (!fs.existsSync(privateDir)) {
  fs.mkdirSync(privateDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    if (file.fieldname === 'coverImage') {
      cb(null, publicDir);
    } else if (file.fieldname === 'digitalFile') {
      cb(null, privateDir);
    } else {
      cb(null, publicDir);
    }
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname);
    cb(null, `${file.fieldname}-${uniqueSuffix}${ext}`);
  }
});

const fileFilter = (req, file, cb) => {
  if (file.fieldname === 'coverImage') {
    const allowedImages = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (allowedImages.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('Invalid image file format. Allowed: JPG, PNG, WEBP, GIF'), false);
    }
  } else if (file.fieldname === 'digitalFile') {
    // Allowed extensions for digital products
    const allowedExts = ['.pdf', '.zip', '.rar', '.epub', '.mp3', '.mp4', '.png', '.jpg', '.txt', '.doc', '.docx'];
    const ext = path.extname(file.originalname).toLowerCase();
    if (allowedExts.includes(ext)) {
      cb(null, true);
    } else {
      cb(new Error('Unsupported digital file extension. Allowed: PDF, ZIP, RAR, EPUB, MP3, MP4, images, DOC'), false);
    }
  } else {
    cb(null, true);
  }
};

const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 50 * 1024 * 1024 // 50 MB limit
  }
});

const productUploads = upload.fields([
  { name: 'coverImage', maxCount: 1 },
  { name: 'digitalFile', maxCount: 1 }
]);

module.exports = {
  productUploads,
  publicDir,
  privateDir
};
