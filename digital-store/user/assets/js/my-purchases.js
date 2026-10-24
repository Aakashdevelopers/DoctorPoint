// My Purchases Controller - Database Only Mode (No Dummy/Mock Data)

document.addEventListener('DOMContentLoaded', async () => {
  const token = await Auth.getToken();

  if (!token) {
    window.location.href = `login.html?redirect=my-purchases.html`;
    return;
  }

  fetchMyPurchases(token);
});

async function fetchMyPurchases(token) {
  const container = document.getElementById('purchases-container');
  if (!container) return;

  try {
    const res = await fetch('/api/purchases/my-purchases', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!res.ok) {
      throw new Error(`Server status ${res.status}`);
    }

    const data = await res.json();

    if (data.success && Array.isArray(data.data)) {
      renderPurchases(data.data, token);
      return;
    }
  } catch (err) {
    console.error('Error loading purchases from database:', err);
    container.innerHTML = `
      <div style="text-align: center; padding: 4rem 1rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
        <i class="fa-solid fa-database" style="font-size: 3rem; color: #ef4444; margin-bottom: 1rem;"></i>
        <h3 style="font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;">Database Connection Required</h3>
        <p style="color: var(--text-muted);">Could not fetch purchases from backend server.</p>
      </div>
    `;
    return;
  }

  renderPurchases([], token);
}

function renderPurchases(purchases, token) {
  const container = document.getElementById('purchases-container');
  if (!container) return;

  if (purchases.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 4rem 1rem; background: white; border-radius: var(--radius); border: 1px solid var(--border-color);">
        <i class="fa-solid fa-cloud-arrow-down" style="font-size: 3.5rem; color: #cbd5e1; margin-bottom: 1rem;"></i>
        <h3 style="font-weight: 700; color: var(--text-main); margin-bottom: 0.5rem;">No Purchases Yet</h3>
        <p style="color: var(--text-muted); margin-bottom: 1.5rem;">Explore our storefront and grab your first digital product with instant access!</p>
        <a href="index.html" class="btn btn-primary"><i class="fa-solid fa-store"></i> Browse Storefront</a>
      </div>
    `;
    return;
  }

  container.innerHTML = purchases.map(p => {
    const prod = p.product || {};
    const formattedDate = new Date(p.purchaseDate).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
    const resourceLink = prod.resourceUrl || 'https://drive.google.com/';

    return `
      <div class="purchase-card">
        <div class="purchase-left">
          <img src="${prod.coverImageUrl || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80'}" alt="${prod.title || 'Digital Product'}" class="purchase-thumb" onerror="this.src='https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80'">
          <div>
            <span class="purchase-badge"><i class="fa-solid fa-circle-check"></i> Granted</span>
            <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-main); margin: 0.3rem 0;">${prod.title || 'Digital Product'}</h3>
            <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 0.3rem;">
              Purchased on ${formattedDate} &bull; ${prod.fileSizeFormatted || 'Digital File'}
            </p>
            <span style="font-size: 0.78rem; background: #fef3c7; color: #92400e; padding: 0.15rem 0.5rem; border-radius: 4px; font-weight: 600;">
              Instant Access Grant
            </span>
          </div>
        </div>

        <div style="display: flex; gap: 0.6rem; align-items: center; flex-wrap: wrap;">
          <a href="${resourceLink}" target="_blank" class="btn btn-success" style="padding: 0.6rem 1rem; font-size: 0.88rem;">
            <i class="fa-solid fa-arrow-up-right-from-square"></i> Open Resource
          </a>
          <button onclick="downloadFile('${p.purchaseId}', '${prod.fileName || 'digital-product.zip'}', '${token}')" class="btn btn-primary" style="padding: 0.6rem 1.2rem; font-size: 0.88rem;">
            <i class="fa-solid fa-download"></i> Download File
          </button>
        </div>
      </div>
    `;
  }).join('');
}

async function downloadFile(purchaseId, fileName, token) {
  try {
    const res = await fetch(`/api/purchases/${purchaseId}/download`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (res.ok) {
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName || 'digital-product';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      a.remove();
      return;
    }
  } catch (err) {
    console.error('Download stream failed:', err);
    alert('Failed to stream digital file download from server.');
  }
}
