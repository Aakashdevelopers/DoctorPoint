// SuperProfile Product Detail Controller - Database Only Mode (No Dummy/Mock Data)

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
    if (!res.ok) {
      throw new Error(`Product request failed with status ${res.status}`);
    }

    const data = await res.json();

    if (data.success && data.data) {
      currentProduct = data.data;
      renderSuperProfileProduct(currentProduct);
      return;
    }
  } catch (err) {
    console.error('Error loading product details from database:', err);
  }

  container.innerHTML = `
    <div style="text-align: center; padding: 4rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
      <i class="fa-solid fa-triangle-exclamation" style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;"></i>
      <h2>Product Not Found</h2>
      <p style="color: var(--text-muted); margin-bottom: 1.5rem;">The requested product could not be loaded from the database.</p>
      <a href="index.html" class="btn btn-primary">Return to Storefront</a>
    </div>
  `;
}

function renderTestimonialsSection(testimonials) {
  if (!testimonials || !Array.isArray(testimonials) || testimonials.length === 0) {
    return '';
  }

  const cardsHtml = testimonials.map(r => {
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
  if (!faqs || !Array.isArray(faqs) || faqs.length === 0) {
    return '';
  }

  const faqsHtml = faqs.map((faq, idx) => `
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
  const resourceLink = p.resourceUrl || 'https://drive.google.com/';

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

  let rightCheckoutCardHtml = '';

  if (p.isPurchased) {
    rightCheckoutCardHtml = `
      <div class="sp-checkout-card" style="background:#f0fdf4; border:1.5px solid #86efac; padding:2.2rem 1.8rem; text-align:center;">
        <div style="width: 52px; height: 52px; background: #166534; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.4rem; margin: 0 auto 1.2rem;">
          <i class="fa-solid fa-check"></i>
        </div>
        <h3 style="color: #14532d; font-weight: 800; font-size: 1.5rem; margin-bottom: 0.4rem; letter-spacing: -0.3px;">You Own This Product</h3>
        <p style="color: #166534; font-size: 0.95rem; font-weight: 600; margin-bottom: 1.8rem;">Instant Demo Access Granted & Active</p>

        <div style="display: flex; flex-direction: column; gap: 0.8rem;">
          <a href="${resourceLink}" target="_blank" class="sp-btn-get-it-now" style="background: #16a34a; padding: 1.05rem; border-radius: 14px; font-size: 1.1rem; font-weight: 800; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);">
            <i class="fa-solid fa-arrow-up-right-from-square"></i> Open Resource URL
          </a>
          <a href="my-purchases.html" class="btn btn-outline" style="background: white; border: 1.5px solid #86efac; color: #14532d; font-weight: 700; border-radius: 14px; padding: 0.9rem; font-size: 1rem; width: 100%;">
            <i class="fa-solid fa-box"></i> View in My Purchases
          </a>
        </div>
      </div>
    `;
  } else {
    rightCheckoutCardHtml = `
      <div class="sp-checkout-card">
        <p class="sp-checkout-notice">Access to this purchase will be sent to this email</p>

        <div class="sp-field-group">
          <span class="sp-floating-label">Email Address</span>
          <input type="email" id="checkout-email" class="sp-input" value="${defaultEmail}" placeholder="you@example.com" required>
        </div>

        <div class="sp-field-group">
          <span class="sp-floating-label">Phone number *</span>
          <div class="sp-phone-wrap">
            <select class="sp-country-select" id="checkout-country">
              <option value="+91">+91</option>
              <option value="+1">+1</option>
              <option value="+44">+44</option>
              <option value="+971">+971</option>
            </select>
            <input type="tel" id="checkout-phone" class="sp-input" placeholder="Phone Number" required>
          </div>
        </div>

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

        <div id="action-box">
          <button onclick="handleInstantBuy('${p._id}')" id="buy-btn" class="sp-btn-get-it-now">
            <span>Get it now</span> <i class="fa-solid fa-arrow-right"></i>
          </button>
        </div>

        <p style="font-size: 0.78rem; color: #64748b; text-align: center; margin-top: 0.8rem;">
          <i class="fa-solid fa-shield-halved"></i> Instant Access Grant — No Payment Processed
        </p>
      </div>
    `;
  }

  container.innerHTML = `
    <div class="sp-product-container">

      <!-- LEFT COLUMN: Product Content -->
      <div class="sp-left-content">
        <div class="sp-banner-wrap">
          <img src="${p.coverImageUrl}" alt="${p.title}" class="sp-banner-img" onerror="this.src='https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80'">
        </div>

        <h1 class="sp-product-title">${p.title}</h1>

        <div class="sp-section-heading">ABOUT THE PAGE</div>
        <p class="sp-description">${p.shortDescription}</p>

        ${featuresHtml}

        <p class="sp-description">${p.description}</p>

        ${renderTestimonialsSection(p.testimonials)}

        ${renderFaqsSection(p.faqs)}

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
            <span class="sp-table-val">${p.fileSizeFormatted || 'Digital Download'}</span>
          </div>
        </div>

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
        ${rightCheckoutCardHtml}

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

  if (!token) {
    const currentUrl = encodeURIComponent(`product.html?slug=${currentProduct ? currentProduct.slug : ''}`);
    window.location.href = `login.html?redirect=${currentUrl}`;
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
    buyBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Granting Access...`;
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
      if (currentProduct) {
        currentProduct.isPurchased = true;
      }
      showSuccessGrant(data.data ? data.data.resourceUrl : null);
      return;
    } else {
      alert(data.error || 'Could not grant purchase access');
    }
  } catch (err) {
    console.error('Error processing purchase with backend:', err);
    alert('Failed to connect to backend server for purchase grant.');
  }

  if (buyBtn) {
    buyBtn.disabled = false;
    buyBtn.innerHTML = `<span>Get it now</span> <i class="fa-solid fa-arrow-right"></i>`;
  }
}

function showSuccessGrant(resourceUrl) {
  const url = resourceUrl || (currentProduct ? currentProduct.resourceUrl : 'https://drive.google.com/');
  const checkoutCard = document.querySelector('.sp-checkout-card');

  if (checkoutCard) {
    checkoutCard.style.background = '#f0fdf4';
    checkoutCard.style.border = '1.5px solid #86efac';
    checkoutCard.style.padding = '2.2rem 1.8rem';
    checkoutCard.style.textAlign = 'center';
    checkoutCard.innerHTML = `
      <div style="width: 52px; height: 52px; background: #166534; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.4rem; margin: 0 auto 1.2rem;">
        <i class="fa-solid fa-check"></i>
      </div>
      <h3 style="color: #14532d; font-weight: 800; font-size: 1.5rem; margin-bottom: 0.4rem; letter-spacing: -0.3px;">You Own This Product</h3>
      <p style="color: #166534; font-size: 0.95rem; font-weight: 600; margin-bottom: 1.8rem;">Instant Access Granted & Active</p>

      <div style="display: flex; flex-direction: column; gap: 0.8rem;">
        <a href="${url}" target="_blank" class="sp-btn-get-it-now" style="background: #16a34a; padding: 1.05rem; border-radius: 14px; font-size: 1.1rem; font-weight: 800; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.25);">
          <i class="fa-solid fa-arrow-up-right-from-square"></i> Open Resource URL
        </a>
        <a href="my-purchases.html" class="btn btn-outline" style="background: white; border: 1.5px solid #86efac; color: #14532d; font-weight: 700; border-radius: 14px; padding: 0.9rem; font-size: 1rem; width: 100%;">
          <i class="fa-solid fa-box"></i> View in My Purchases
        </a>
      </div>
    `;

    try {
      window.open(url, '_blank');
    } catch (e) {}
  }
}
