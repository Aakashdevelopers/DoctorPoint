const path = require('path');
const fs = require('fs');

class StorageService {
  /**
   * Resolves the full filesystem path for a private file or generates signed URL
   */
  static getPrivateFilePath(filePath) {
    if (!filePath) return null;

    // Absolute path
    if (path.isAbsolute(filePath)) {
      return fs.existsSync(filePath) ? filePath : null;
    }

    // Relative to server root
    const resolvedPath = path.resolve(__dirname, '../../', filePath);
    if (fs.existsSync(resolvedPath)) {
      return resolvedPath;
    }

    // Relative to uploads/private
    const privateUploadsPath = path.resolve(__dirname, '../../uploads/private', path.basename(filePath));
    if (fs.existsSync(privateUploadsPath)) {
      return privateUploadsPath;
    }

    return null;
  }

  /**
   * Generates public cover image URL or path
   */
  static getPublicImageUrl(coverPath, req) {
    if (!coverPath) return '/assets/images/placeholder.png';
    if (coverPath.startsWith('http://') || coverPath.startsWith('https://')) {
      return coverPath;
    }
    const filename = path.basename(coverPath);
    return `/uploads/public/${filename}`;
  }
}

module.exports = StorageService;
