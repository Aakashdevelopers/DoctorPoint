# NovaStore — Professional Digital Product Store (Phase 1)

A full-stack, responsive digital product storefront and management platform inspired by SuperProfile. Built with Node.js, Express, MongoDB Atlas, and Firebase Authentication.

---

## 🚀 Features Implemented (Phase 1)

1. **Public Storefront**: Modern hero section, search, category filters, responsive product cards, loading skeletons, and empty state messaging.
2. **Product Details**: Product information, included file metadata, pricing in INR, and interactive Buy Now.
3. **Instant Demo Purchasing**:
   - One-click purchase workflow without payment gateway calls.
   - Server-side validation (price and eligibility strictly server-authoritative).
   - Idempotent grant (`purchaseType: "demo"`, `status: "granted"`).
   - Prevents duplicate purchases using compound unique MongoDB index (`userId` + `productId`).
   - Clear "Demo Purchase — No Payment Processed" label.
4. **My Purchases**:
   - Secure digital product library for authenticated users.
   - Authorized file downloads via temporary streaming/signed access.
5. **Admin Control Panel (`/admin`)**:
   - Firebase Auth + Server-side role/email verification.
   - Overview stats: Total products, published count, demo purchase count, activity log.
   - Product Management: Add, edit, publish, unpublish, and safe archiving.
   - Digital file & cover image upload with progress feedback.
   - Demo Purchase audit table (No fake revenue metrics).
6. **Security & Data Integrity**:
   - Firebase ID token verification using Firebase Admin SDK.
   - MongoDB ObjectIds and payload validation.
   - Protection against unauthorized digital file access.
   - Unpublished and archived products blocked from purchasing.

---

## 🛠️ Technology Stack

- **Frontend**: HTML5, Custom CSS3 (Inter & Plus Jakarta Sans typography), ES6 JavaScript
- **Backend**: Node.js, Express.js, Mongoose, Firebase Admin SDK
- **Database**: MongoDB Atlas
- **Storage**: Private Firebase Storage / Local Private Storage Fallback
- **Authentication**: Firebase Authentication

---

## 📁 Directory Structure

```
digital-store/
├── client/                      # Frontend web application
│   ├── index.html               # Storefront homepage
│   ├── product.html             # Product details & Instant Buy
│   ├── my-purchases.html        # User purchases library
│   ├── login.html               # Authentication page
│   ├── admin/                   # Admin panel interface
│   │   ├── index.html           # Admin dashboard & manage products
│   │   ├── add-product.html     # Add product form
│   │   └── edit-product.html    # Edit product form
│   └── assets/                  # Frontend assets
│       ├── css/                 # Modern custom CSS stylesheets
│       │   ├── styles.css
│       │   └── admin.css
│       └── js/                  # Frontend ES modules
│           ├── firebase-config.js
│           ├── auth.js
│           ├── store.js
│           ├── product.js
│           ├── my-purchases.js
│           └── admin.js
├── server/                      # Node.js + Express backend
│   ├── src/
│   │   ├── config/              # MongoDB & Firebase Admin configuration
│   │   │   ├── db.js
│   │   │   └── firebase.js
│   │   ├── controllers/         # Business logic & API endpoints
│   │   │   ├── productController.js
│   │   │   ├── purchaseController.js
│   │   │   ├── adminController.js
│   │   │   └── authController.js
│   │   ├── middleware/          # Security & upload middlewares
│   │   │   ├── auth.js
│   │   │   ├── upload.js
│   │   │   └── errorHandler.js
│   │   ├── models/              # Mongoose database models
│   │   │   ├── Product.js
│   │   │   ├── UserProfile.js
│   │   │   └── Purchase.js
│   │   ├── routes/              # Express route definitions
│   │   │   ├── productRoutes.js
│   │   │   ├── purchaseRoutes.js
│   │   │   ├── adminRoutes.js
│   │   │   └── authRoutes.js
│   │   ├── services/            # File storage abstraction service
│   │   │   └── storageService.js
│   │   ├── utils/               # Seed data generator & helpers
│   │   │   └── seedData.js
│   │   ├── app.js               # Express application initialization
│   │   └── server.js            # HTTP server entry point
│   ├── uploads/                 # Local uploads directory (Dev mode)
│   │   ├── public/
│   │   └── private/
│   ├── package.json
│   └── .env.example
└── README.md
```

---

## ⚙️ Installation & Setup

### 1. Prerequisites
- Node.js (v18 or higher)
- MongoDB Atlas cluster or local MongoDB instance
- Firebase Project with Authentication (Email/Password) enabled

### 2. Environment Setup
In `digital-store/server/`:
```bash
cp .env.example .env
```
Fill in your MongoDB URI and Firebase Service Account details in `.env`.

In `digital-store/client/assets/js/firebase-config.js`:
Add your Firebase Web Config values (`apiKey`, `authDomain`, `projectId`, etc.).

### 3. Install Dependencies
```bash
cd digital-store/server
npm install
```

### 4. Seed Development Data (Optional)
To populate sample products and an admin user for immediate testing:
```bash
npm run seed
```

### 5. Start the Server
```bash
npm run dev
# or
npm start
```
The server will run at `http://localhost:5000` and automatically serve the static web frontend from `http://localhost:5000/`.

---

## 🧪 Testing the Instant Demo Buy Workflow

1. Open `http://localhost:5000/admin` in your browser to log in as Admin (`admin@novastore.com`).
2. Add a new digital product with a title, price (in INR), cover image, and private product file. Set status to **Published**.
3. Go to the storefront homepage at `http://localhost:5000/`. Confirm your product is listed.
4. Click **View Details** on the product card.
5. Click **Buy Now**. If not logged in, you will be redirected to Sign In / Sign Up, and returned automatically to complete the purchase.
6. Observe the Instant Purchase confirmation. Notice the label **"Demo Purchase — No Payment Processed"**.
7. Navigate to **My Purchases**. Verify the product appears in your library.
8. Click **Download File**. The backend verifies ownership before serving the private file.
9. Click **Buy Now** again on the same product page; the system safely redirects you to "Open Product" without duplicating the purchase record.
