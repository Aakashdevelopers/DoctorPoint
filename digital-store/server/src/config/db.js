const mongoose = require('mongoose');

const connectDB = async () => {
  try {
    const connStr = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/novastore';
    const conn = await mongoose.connect(connStr);
    console.log(`[MongoDB] Connected: ${conn.connection.host}/${conn.connection.name}`);
  } catch (err) {
    console.error(`[MongoDB] Connection Error: ${err.message}`);
    // If running in development without live Mongo DB, log helpful instructions
    if (process.env.NODE_ENV === 'development') {
      console.warn('[MongoDB] Warning: Database connection failed. Ensure MONGODB_URI is configured correctly in .env.');
    } else {
      process.exit(1);
    }
  }
};

module.exports = connectDB;
