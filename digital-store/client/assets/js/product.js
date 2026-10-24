// SuperProfile Product Detail Controller

let currentProduct = null;

document.addEventListener('DOMContentLoaded', () => {
  const urlParams = new URLSearchParams(window.location.search);
  const slug = urlParams.get('slug');

  if (!slug) {
    window.location.href = 'index.html';
    return;
  }

  fetchProductDetails(slug);
});

async function fetchProductDetails(slug) {
  const container = document.getElementById('product-detail-container');
  if (!container) return;

  try {
    const token = await Auth.getToken();
    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`/api/products/${slug}`, { headers });
    const data = await res.json();

    if (!data.success) {
      container.innerHTML = `
        <div style="text-align: center; padding: 4rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
          <i class="fa-solid fa-triangle-exclamation" style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;"></i>
          <h2>Product Not Found</h2>
          <p style="color: var(--text-muted); margin-bottom: 1.5rem;">${data.error || 'The requested product is unavailable.'}</p>
          <a href="index.html" class="btn btn-primary">Return to Storefront</a>
        </div>
      `;
      return;
    }

    currentProduct = data.data;
    renderSuperProfileProduct(currentProduct);
  } catch (err) {
    console.error('Error loading product details:', err);
    container.innerHTML = `<div style="text-align:center;padding:3rem;"><p>Unable to load product details. Server unavailable.</p></div>`;
  }
}

function renderTestimonialsSection(testimonials) {
  const defaultReviews = [
    {
      authorName: 'Dr. Akshay Gawande (MBBS)',
      authorRole: 'Medical Practitioner',
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
  ];

  const reviewsToRender = (testimonials && testimonials.length > 0) ? testimonials : defaultReviews;

  const cardsHtml = reviewsToRender.map(r => {
    const ratingVal = Math.min(5, Math.max(1, parseInt(r.rating) || 5));
    const starsHtml = '<i class="fa-solid fa-star"></i>'.repeat(ratingVal);
    const avatarInner = r.avatarUrl
      ? `<img src="${r.avatarUrl}" alt="${r.authorName}">`
      : `😀`;

    return `
      <div class="sp-testimonial-card">
        <div>
          <div class="sp-testimonial-top">
            <div class="sp-stars">${starsHtml}</div>
            <i class="fa-solid fa-quote-right sp-quote-icon"></i>
          </div>
          <p class="sp-testimonial-comment">${r.comment}</p>
        </div>
        <div class="sp-testimonial-author">
          <div class="sp-author-avatar">${avatarInner}</div>
          <div>
            <div class="sp-author-name">${r.authorName}</div>
            ${r.authorRole ? `<div class="sp-author-role">${r.authorRole}</div>` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');

  return `
    <div class="sp-section-heading" style="margin-top: 2rem;">TESTIMONIALS</div>
    <div class="sp-testimonials-grid">
      ${cardsHtml}
    </div>
  `;
}

function renderFaqsSection(faqs) {
  const defaultFaqs = [
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
  ];

  const faqsToRender = (faqs && faqs.length > 0) ? faqs : defaultFaqs;

  const faqsHtml = faqsToRender.map((faq, idx) => `
    <div class="sp-faq-item" id="faq-item-${idx}">
      <div class="sp-faq-header" onclick="toggleFaq(${idx})">
        <span>${faq.question}</span>
        <i class="fa-solid fa-chevron-down sp-faq-arrow"></i>
      </div>
      <div class="sp-faq-body">
        ${faq.answer}
      </div>
    </div>
  `).join('');

  return `
    <div class="sp-section-heading" style="margin-top: 2.5rem;">FREQUENTLY ASKED QUESTIONS</div>
    <div class="sp-faq-list">
      ${faqsHtml}
    </div>
  `;
}

window.toggleFaq = function(idx) {
  const item = document.getElementById(`faq-item-${idx}`);
  if (item) {
    item.classList.toggle('active');
  }
};

function renderSuperProfileProduct(p) {
  const container = document.getElementById('product-detail-container');
  const user = Auth.getUser();
  const defaultEmail = user ? user.email : '';

  // Feature items rendering with checkmarks
  let featuresHtml = '';
  if (p.features && p.features.length) {
    featuresHtml = `
      <ul class="sp-feature-list">
        ${p.features.map(f => `
          <li class="sp-feature-item">
            <i class="fa-solid fa-check"></i>
            <span>${f}</span>
          </li>
        `).join('')}
      </ul>
    `;
  }

  // CTA Button rendering
  const buyButtonHtml = p.isPurchased
    ? `<a href="my-purchases.html" class="sp-btn-get-it-now" style="background:#16a34a;">
         <span>Open Product</span> <i class="fa-solid fa-arrow-right"></i>
       </a>`
    : `<button onclick="handleInstantBuy('${p._id}')" id="buy-btn" class="sp-btn-get-it-now">
         <span>Get it now</span> <i class="fa-solid fa-arrow-right"></i>
       </button>`;

  container.innerHTML = `
    <div class="sp-product-container">

      <!-- LEFT COLUMN: Product Content -->
      <div class="sp-left-content">
        <!-- Banner / Cover Image -->
        <div class="sp-banner-wrap">
          <img src="${p.coverImageUrl}" alt="${p.title}" class="sp-banner-img" onerror="this.src='/assets/images/placeholder.png'">
        </div>

        <!-- Product Title -->
        <h1 class="sp-product-title">${p.title}</h1>

        <!-- About Section -->
        <div class="sp-section-heading">ABOUT THE PAGE</div>
        <p class="sp-description">${p.shortDescription}</p>

        <!-- Feature List with Checkmarks -->
        ${featuresHtml}

        <!-- Detailed Description -->
        <p class="sp-description">${p.description}</p>

        ${renderTestimonialsSection(p.testimonials)}

        ${renderFaqsSection(p.faqs)}

        <!-- WHAT YOU'LL GET Table -->
        <div class="sp-section-heading">WHAT YOU'LL GET</div>
        <div class="sp-table-box">
          <div class="sp-table-row">
            <span class="sp-table-label">Number of resources</span>
            <span class="sp-table-val">${p.resourceCount || 1}</span>
          </div>
          <div class="sp-table-row">
            <span class="sp-table-label">Resource content</span>
            <span class="sp-table-val">${p.resourceType || 'File'}</span>
          </div>
          <div class="sp-table-row">
            <span class="sp-table-label">Total file size</span>
            <span class="sp-table-val">${p.fileSizeFormatted}</span>
          </div>
        </div>

        <!-- CONTACT ME Section -->
        <div class="sp-section-heading">CONTACT ME FOR ANY ISSUES OR QUERIES</div>
        <div class="sp-contact-box">
          <div class="sp-contact-item">
            <i class="fa-regular fa-envelope"></i>
            <span>${p.supportEmail || 'support@novastore.com'}</span>
          </div>
          <div class="sp-contact-item">
            <i class="fa-solid fa-phone"></i>
            <span>${p.supportPhone || '+91 9876543210'}</span>
          </div>
        </div>
      </div>

      <!-- RIGHT COLUMN: Sticky Checkout Card -->
      <div class="sp-sticky-col">
        <div class="sp-checkout-card">
          <p class="sp-checkout-notice">Access to this purchase will be sent to this email</p>

          <!-- Email Input Field -->
          <div class="sp-field-group">
            <span class="sp-floating-label">Email Address</span>
            <input type="email" id="checkout-email" class="sp-input" value="${defaultEmail}" placeholder="you@example.com" required>
          </div>

          <!-- Phone Number Input Field -->
          <div class="sp-field-group">
            <span class="sp-floating-label">Phone number *</span>
            <div class="sp-phone-wrap">
              <select class="sp-country-select" id="checkout-country">
                <option value="+91">+91</option>
                <option value="+1">+1</option>
                <option value="+44">+44</option>
                <option value="+971">+971</option>
              </select>
              <input type="tel" id="checkout-phone" class="sp-input" value="6307277415" placeholder="Phone Number" required>
            </div>
          </div>

          <!-- Price Breakdown -->
          <div class="sp-price-row">
            <span>Sub Total</span>
            <div>
              ${p.compareAtPriceInINR ? `<span style="text-decoration: line-through; color: #94a3b8; margin-right: 0.4rem;">₹${p.compareAtPriceInINR}</span>` : ''}
              <span style="font-weight: 700; color: #0f172a;">₹${p.priceInINR}</span>
            </div>
          </div>

          <div class="sp-total-row">
            <span>Total</span>
            <span style="font-size: 1.3rem;">₹${p.priceInINR}</span>
          </div>

          <!-- Checkout CTA Button -->
          <div id="action-box">
            ${buyButtonHtml}
          </div>

          <p style="font-size: 0.78rem; color: #64748b; text-align: center; margin-top: 0.8rem;">
            <i class="fa-solid fa-shield-halved"></i> Demo Purchase — No payment charged
          </p>
        </div>

        <!-- Copy Link Card -->
        <div class="sp-copy-card">
          <span class="sp-copy-heading">INVITE YOUR NETWORK</span>
          <button onclick="copyProductLink()" class="sp-btn-copy" id="copy-btn">
            <i class="fa-solid fa-link"></i> <span>Copy link</span>
          </button>
        </div>
      </div>

    </div>
  `;
}

function copyProductLink() {
  navigator.clipboard.writeText(window.location.href);
  const copyBtn = document.getElementById('copy-btn');
  if (copyBtn) {
    copyBtn.innerHTML = `<i class="fa-solid fa-check" style="color:#16a34a;"></i> <span>Copied!</span>`;
    setTimeout(() => {
      copyBtn.innerHTML = `<i class="fa-solid fa-link"></i> <span>Copy link</span>`;
    }, 2000);
  }
}

async function handleInstantBuy(productId) {
  const token = await Auth.getToken();

  // Redirect to login if user is unauthenticated
  if (!token) {
    const loginPath = window.location.protocol === 'file:' ? 'login.html' : 'login.html';
    const currentUrl = encodeURIComponent(`product.html?slug=${currentProduct ? currentProduct.slug : ''}`);
    window.location.href = `${loginPath}?redirect=${currentUrl}`;
    return;
  }

  const emailInput = document.getElementById('checkout-email');
  const phoneInput = document.getElementById('checkout-phone');
  const countrySelect = document.getElementById('checkout-country');

  const customerEmail = emailInput ? emailInput.value.trim() : '';
  const phoneNumber = phoneInput && countrySelect ? `${countrySelect.value} ${phoneInput.value.trim()}` : '';

  const buyBtn = document.getElementById('buy-btn');
  if (buyBtn) {
    buyBtn.disabled = true;
    buyBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Processing Grant...`;
  }

  try {
    const res = await fetch('/api/purchases/demo-buy', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({ productId, customerEmail, phoneNumber })
    });

    const data = await res.json();

    if (data.success) {
      const actionBox = document.getElementById('action-box');
      actionBox.innerHTML = `
        <div style="background: #dcfce7; border: 1px solid #86efac; border-radius: 12px; padding: 1rem; text-align: center; margin-bottom: 0.8rem;">
          <i class="fa-solid fa-circle-check" style="font-size: 1.8rem; color: #166534; margin-bottom: 0.3rem;"></i>
          <h4 style="color: #14532d; font-weight: 700;">Purchase Granted!</h4>
          <p style="color: #166534; font-size: 0.8rem; margin-bottom: 0.8rem;">Demo Purchase — No Payment Processed</p>
          <a href="my-purchases.html" class="sp-btn-get-it-now" style="background:#16a34a; padding: 0.75rem;">
            <span>Go to My Purchases</span> <i class="fa-solid fa-arrow-right"></i>
          </a>
        </div>
      `;
    } else {
      alert(`Purchase failed: ${data.error || 'Server error'}`);
      if (buyBtn) {
        buyBtn.disabled = false;
        buyBtn.innerHTML = `<span>Get it now</span> <i class="fa-solid fa-arrow-right"></i>`;
      }
    }
  } catch (err) {
    console.error('Error making demo purchase:', err);
    alert('An unexpected error occurred during demo purchase.');
    if (buyBtn) {
      buyBtn.disabled = false;
      buyBtn.innerHTML = `<span>Get it now</span> <i class="fa-solid fa-arrow-right"></i>`;
    }
  }
}
