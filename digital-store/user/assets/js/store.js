// Storefront Controller - Database Only Mode (No Dummy/Mock Data)

let currentCategory = 'All';
let searchQuery = '';

document.addEventListener('DOMContentLoaded', () => {
  fetchCategories();
  fetchProducts();

  const searchInput = document.getElementById('search-input');
  if (searchInput) {
    let debounceTimeout;
    searchInput.addEventListener('input', (e) => {
      clearTimeout(debounceTimeout);
      debounceTimeout = setTimeout(() => {
        searchQuery = e.target.value.trim();
        fetchProducts();
      }, 300);
    });
  }
});

async function fetchCategories() {
  try {
    const res = await fetch('/api/products/categories');
    const data = await res.json();
    if (data.success && data.data && Array.isArray(data.data)) {
      renderCategoryPills(data.data);
      return;
    }
  } catch (err) {
    console.error('Error fetching categories from database:', err);
  }

  renderCategoryPills(['All']);
}

function renderCategoryPills(categories) {
  const container = document.getElementById('category-pills');
  if (!container) return;

  container.innerHTML = categories.map(cat => `
    <button class="pill ${cat === currentCategory ? 'active' : ''}" data-category="${cat}">
      ${cat}
    </button>
  `).join('');

  container.querySelectorAll('.pill').forEach(btn => {
    btn.addEventListener('click', (e) => {
      container.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
      e.target.classList.add('active');
      currentCategory = e.target.getAttribute('data-category');
      fetchProducts();
    });
  });
}

async function fetchProducts() {
  const grid = document.getElementById('product-grid');
  if (!grid) return;

  grid.innerHTML = Array(3).fill('<div class="skeleton-card"></div>').join('');

  try {
    const params = new URLSearchParams();
    if (currentCategory && currentCategory !== 'All') params.append('category', currentCategory);
    if (searchQuery) params.append('search', searchQuery);

    const res = await fetch(`/api/products?${params.toString()}`);
    if (!res.ok) {
      throw new Error(`Server returned status ${res.status}`);
    }

    const data = await res.json();

    if (data.success && Array.isArray(data.data)) {
      renderProducts(data.data);
      return;
    }
  } catch (err) {
    console.error('Error fetching products from MySQL database:', err);
    grid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
        <i class="fa-solid fa-database" style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;"></i>
        <h3 style="font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;">Database Connection Required</h3>
        <p style="color: var(--text-muted);">Could not connect to backend server. Please verify MySQL configuration and deployment.</p>
      </div>
    `;
    return;
  }

  renderProducts([]);
}

function renderProducts(products) {
  const grid = document.getElementById('product-grid');
  if (!grid) return;

  if (products.length === 0) {
    grid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 4rem 1rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
        <i class="fa-solid fa-box-open" style="font-size: 3rem; color: #cbd5e1; margin-bottom: 1rem;"></i>
        <h3 style="font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;">No Digital Products Found</h3>
        <p style="color: var(--text-muted);">No items match your search or category filter. Add products from the Admin Panel.</p>
      </div>
    `;
    return;
  }

  grid.innerHTML = products.map(p => `
    <div class="product-card">
      <div class="card-image-wrap">
        <img src="${p.coverImageUrl}" alt="${p.title}" class="card-image" onerror="this.src='https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80'">
        <span class="card-category">${p.category}</span>
      </div>
      <div class="card-body">
        <h3 class="card-title">${p.title}</h3>
        <p class="card-desc">${p.shortDescription}</p>
        <div class="card-footer">
          <div>
            <span class="price-tag">₹${p.priceInINR}</span>
            ${p.compareAtPriceInINR ? `<span class="compare-price">₹${p.compareAtPriceInINR}</span>` : ''}
          </div>
          <a href="product.html?slug=${p.slug}" class="btn btn-outline" style="padding: 0.5rem 1rem; font-size: 0.88rem;">
            View Details
          </a>
        </div>
      </div>
    </div>
  `).join('');
}
