const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../.env') });

const app = require('./app');
const connectDB = require('./config/db');

const PORT = process.env.PORT || 5000;

// Initialize Database & Server
const startServer = async () => {
  await connectDB();

  app.listen(PORT, () => {
    console.log(`==================================================`);
    console.log(`🚀 NovaStore Server running in ${process.env.NODE_ENV || 'development'} mode`);
    console.log(`🌐 Server URL: http://localhost:${PORT}`);
    console.log(`🛍️  Storefront: http://localhost:${PORT}/`);
    console.log(`🔐 Admin Panel: http://localhost:${PORT}/admin`);
    console.log(`==================================================`);
  });
};

startServer();
