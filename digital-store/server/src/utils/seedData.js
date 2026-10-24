const path = require('path');
require('dotenv').config({ path: path.resolve(__dirname, '../../.env') });

const mongoose = require('mongoose');
const fs = require('fs');
const connectDB = require('../config/db');
const Product = require('../models/Product');
const UserProfile = require('../models/UserProfile');
const Purchase = require('../models/Purchase');

const sampleProducts = [
  {
    title: '5000+ AI Video Prompt Bundle | Cinematic, Viral Reels, Anime, Horror & Fan',
    shortDescription: 'Create stunning AI videos in seconds using 5000+ ready-to-use premium prompts for all major AI video tools.',
    description: '🔥 THE ULTIMATE 5000+ AI VIDEO PROMPT BUNDLE 🔥\n\nLevel up your AI content creation with this massive collection of high-quality cinematic prompts designed for creators, filmmakers, YouTubers, TikTok creators, Instagram reel creators and AI artists.\n\nWorks perfectly with Runway, Kling AI, Pika Labs, Luma AI and Google Veo.',
    features: [
      '5000+ Ready-to-Use AI Video Prompts',
      'Cinematic Movie Style Prompts',
      'Viral Reel & Shorts Prompts',
      'Anime & Fantasy Styles',
      'Horror & Sci-Fi Scenes',
      'Product Ad Prompts',
      'Beginner Friendly',
      'Lifetime Access'
    ],
    testimonials: [
      {
        authorName: 'Dr. Akshay Gawande (MBBS)',
        authorRole: 'Resident Doctor',
        rating: 5,
        comment: 'Frankly speaking, I was doubting before buying. Par andar ka content dekh kar I was blown away. just go for it guys!',
        avatarUrl: ''
      },
      {
        authorName: 'Dr. SHREYA TRIPATHI (BNYS)',
        authorRole: 'Paramedical Doctor',
        rating: 5,
        comment: 'Thank you for sharing this best pdf for my knowledge I am paramedical doctor I will surely learn and give a time to pdf for knowledge..thank you!',
        avatarUrl: ''
      },
      {
        authorName: 'Shruti bhoyar (BAMS 2nd proff)',
        authorRole: 'BAMS Student',
        rating: 5,
        comment: 'this book solves everything! Thankyou Utkarsh for making this. Very nice actually 👍',
        avatarUrl: ''
      },
      {
        authorName: 'Pranjali (MBBS)',
        authorRole: 'Medical Student',
        rating: 5,
        comment: 'Too much worth it!',
        avatarUrl: ''
      }
    ],
    faqs: [
      {
        question: 'Who is this guide for?',
        answer: 'This guide is specially created for medical students, interns, resident doctors, BAMS, BHMS, BNYS, and paramedical practitioners looking for quick, practical clinical reference.'
      },
      {
        question: "What if I'm in 1st year — is this too advanced?",
        answer: 'Not at all! The guide starts from fundamental clinical diagnosis, symptom analysis, and prescription protocols, making it very easy to understand for beginners.'
      },
      {
        question: 'Will I get a PDF or physical book?',
        answer: 'This is an instant digital download. You will get immediate access to the high-resolution, searchable PDF file that you can read on phone, tablet, laptop, or print.'
      },
      {
        question: 'Will I get a physical book?',
        answer: 'This product is a digital PDF edition for instant download. You can download and print it locally if you prefer a paper copy.'
      },
      {
        question: 'Are pediatric doses included?',
        answer: 'Yes, standard adult and pediatric dosage calculations and emergency drug protocols are included.'
      }
    ],
    supportEmail: 'ravivadhiya46@gmail.com',
    supportPhone: '+91 9574055968',
    resourceCount: 1,
    resourceType: 'File',
    coverImagePath: 'sample-prompt-bundle.jpg',
    privateFilePath: 'sample-prompt-bundle.zip',
    fileName: '5000-ai-video-prompts-bundle.zip',
    fileSize: 400384, // ~390.97 KB
    priceInPaise: 49900, // ₹499
    compareAtPriceInPaise: 149900, // ₹1,499
    category: 'Prompts',
    status: 'published'
  },
  {
    title: 'Full-Stack Modern Web Architecture E-Book',
    shortDescription: 'Comprehensive guide covering Node.js, Express, MongoDB Atlas, and System Design patterns.',
    description: 'Master modern web development with this step-by-step masterclass e-book. Learn clean architecture, microservices, secure authentication with OAuth & JWT, and scalable database indexing.',
    features: [
      '350+ pages of high-quality content',
      'Production-ready code snippets',
      'Architectural diagrams and cheat sheets',
      'Lifetime Updates'
    ],
    supportEmail: 'ravivadhiya46@gmail.com',
    supportPhone: '+91 9574055968',
    resourceCount: 1,
    resourceType: 'E-Book',
    coverImagePath: 'sample-ebook-cover.jpg',
    privateFilePath: 'sample-ebook-file.pdf',
    fileName: 'fullstack-web-architecture-guide.pdf',
    fileSize: 12582912, // ~12MB
    priceInPaise: 49900, // ₹499
    compareAtPriceInPaise: 99900, // ₹999
    category: 'E-Books',
    status: 'published'
  },
  {
    title: 'SuperProfile Style SaaS UI Kit & Figma Assets',
    shortDescription: 'Modern, conversion-optimized Figma UI kit for creator economy digital stores and products.',
    description: 'Supercharge your design workflow with over 120+ pre-built mobile & desktop components. Fully responsive layout grids, dark/light theme tokens, and clean typography scales.',
    features: [
      '120+ Responsive UI Components',
      'Light & Dark Mode Tokens',
      'Auto-layout 5.0 Support',
      'Figma Design Tokens'
    ],
    supportEmail: 'ravivadhiya46@gmail.com',
    supportPhone: '+91 9574055968',
    resourceCount: 1,
    resourceType: 'Figma File',
    coverImagePath: 'sample-uikit-cover.jpg',
    privateFilePath: 'sample-uikit-file.zip',
    fileName: 'novastore-creator-uikit.zip',
    fileSize: 28311552, // ~27MB
    priceInPaise: 129900, // ₹1,299
    compareAtPriceInPaise: 249900, // ₹2,499
    category: 'Templates',
    status: 'published'
  }
];

const seed = async () => {
  try {
    await connectDB();

    console.log('[Seed] Clearing old sample data...');
    await Product.deleteMany({});
    await UserProfile.deleteMany({});
    await Purchase.deleteMany({});

    const publicDir = path.resolve(__dirname, '../../uploads/public');
    const privateDir = path.resolve(__dirname, '../../uploads/private');
    if (!fs.existsSync(publicDir)) fs.mkdirSync(publicDir, { recursive: true });
    if (!fs.existsSync(privateDir)) fs.mkdirSync(privateDir, { recursive: true });

    console.log('[Seed] Inserting sample digital products...');
    const insertedProducts = await Product.create(sampleProducts);

    console.log('[Seed] Creating sample user profiles...');
    const adminProfile = await UserProfile.create({
      firebaseUid: 'seed-admin-uid-123',
      email: 'admin@novastore.com',
      displayName: 'System Administrator',
      role: 'admin'
    });

    const customerProfile = await UserProfile.create({
      firebaseUid: 'seed-customer-uid-456',
      email: 'customer@novastore.com',
      displayName: 'Jane Doe',
      role: 'user'
    });

    console.log('==================================================');
    console.log('✅ NovaStore Seed Data Population Complete!');
    console.log(`📦 Inserted ${insertedProducts.length} digital products`);
    console.log('==================================================');

    process.exit(0);
  } catch (err) {
    console.error(`[Seed Error] ${err.message}`);
    process.exit(1);
  }
};

seed();
