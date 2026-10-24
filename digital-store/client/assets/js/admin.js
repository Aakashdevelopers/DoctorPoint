// Admin Panel Controller

const Admin = {
  async getAuthHeaders() {
    const token = await Auth.getToken();
    if (!token) return null;
    return { 'Authorization': `Bearer ${token}` };
  },

  async verifyAdminAccess() {
    const headers = await this.getAuthHeaders();
    if (!headers) {
      const loginPath = window.location.protocol === 'file:' ? '../login.html' : '/login.html';
      const redirectTarget = window.location.protocol === 'file:' ? 'admin/index.html' : '/admin';
      window.location.href = `${loginPath}?redirect=${encodeURIComponent(redirectTarget)}`;
      return false;
    }

    try {
      const res = await fetch('/api/auth/me', { headers });
      const data = await res.json();

      if (!data.success || !data.data.isAdmin) {
        alert('Access denied. Administrator privileges required.');
        window.location.href = window.location.protocol === 'file:' ? '../index.html' : '/';
        return false;
      }
      return true;
    } catch (err) {
      console.error('Admin verification error:', err);
      return false;
    }
  },

  async initDashboard() {
    if (!(await this.verifyAdminAccess())) return;
    this.loadStats();
    this.loadProducts();
    this.loadPurchasesAudit();
  },

  async loadStats() {
    const headers = await this.getAuthHeaders();
    try {
      const res = await fetch('/api/admin/stats', { headers });
      const data = await res.json();

      if (data.success) {
        document.getElementById('stat-total-products').textContent = data.data.totalProducts;
        document.getElementById('stat-published-products').textContent = data.data.publishedProducts;
        document.getElementById('stat-demo-purchases').textContent = data.data.totalDemoPurchases;
        document.getElementById('stat-draft-products').textContent = data.data.draftProducts;
      }
    } catch (err) {
      console.error('Error loading stats:', err);
    }
  },

  async loadProducts(search = '', status = 'all') {
    const tableBody = document.getElementById('products-table-body');
    if (!tableBody) return;

    const headers = await this.getAuthHeaders();
    try {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (status) params.append('status', status);

      const res = await fetch(`/api/admin/products?${params.toString()}`, { headers });
      const data = await res.json();

      if (data.success) {
        this.renderProductsTable(data.data);
      }
    } catch (err) {
      console.error('Error loading admin products:', err);
    }
  },

  renderProductsTable(products) {
    const tableBody = document.getElementById('products-table-body');
    if (products.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">No products found matching filters.</td></tr>`;
      return;
    }

    tableBody.innerHTML = products.map(p => `
      <tr>
        <td>
          <div style="display:flex;align-items:center;gap:0.8rem;">
            <img src="${p.coverImageUrl}" alt="" style="width:42px;height:42px;border-radius:6px;object-fit:cover;">
            <div>
              <strong style="display:block;font-size:0.95rem;">${p.title}</strong>
              <small style="color:var(--text-muted);">${p.category}</small>
            </div>
          </div>
        </td>
        <td><strong>₹${p.priceInINR}</strong></td>
        <td><span class="badge-${p.status}">${p.status.toUpperCase()}</span></td>
        <td>${new Date(p.createdAt).toLocaleDateString()}</td>
        <td>
          <div style="display:flex;gap:0.4rem;">
            <a href="edit-product.html?id=${p._id}" class="btn btn-outline" style="padding:0.35rem 0.7rem;font-size:0.8rem;">
              <i class="fa-solid fa-pen"></i> Edit
            </a>
            ${p.status === 'published' ? `
              <button onclick="Admin.changeStatus('${p._id}', 'draft')" class="btn btn-outline" style="padding:0.35rem 0.7rem;font-size:0.8rem;">
                Unpublish
              </button>
            ` : p.status === 'draft' ? `
              <button onclick="Admin.changeStatus('${p._id}', 'published')" class="btn btn-success" style="padding:0.35rem 0.7rem;font-size:0.8rem;">
                Publish
              </button>
            ` : ''}
            ${p.status !== 'archived' ? `
              <button onclick="Admin.archiveProduct('${p._id}')" class="btn btn-outline" style="padding:0.35rem 0.7rem;font-size:0.8rem;color:#ef4444;border-color:#fca5a5;">
                Archive
              </button>
            ` : ''}
          </div>
        </td>
      </tr>
    `).join('');
  },

  async changeStatus(id, newStatus) {
    const headers = await this.getAuthHeaders();
    headers['Content-Type'] = 'application/json';

    try {
      const res = await fetch(`/api/admin/products/${id}/status`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify({ status: newStatus })
      });
      const data = await res.json();
      if (data.success) {
        this.loadStats();
        this.loadProducts();
      } else {
        alert(`Error: ${data.error}`);
      }
    } catch (err) {
      console.error('Error updating status:', err);
    }
  },

  async archiveProduct(id) {
    if (!confirm('Are you sure you want to archive this product? Archived products will be hidden from the public store, but historical purchase records will be preserved.')) {
      return;
    }

    const headers = await this.getAuthHeaders();
    try {
      const res = await fetch(`/api/admin/products/${id}`, {
        method: 'DELETE',
        headers
      });
      const data = await res.json();
      if (data.success) {
        this.loadStats();
        this.loadProducts();
      } else {
        alert(`Archive failed: ${data.error}`);
      }
    } catch (err) {
      console.error('Error archiving product:', err);
    }
  },

  async loadPurchasesAudit() {
    const tableBody = document.getElementById('purchases-audit-table-body');
    if (!tableBody) return;

    const headers = await this.getAuthHeaders();
    try {
      const res = await fetch('/api/admin/purchases', { headers });
      const data = await res.json();

      if (data.success) {
        if (data.data.length === 0) {
          tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">No demo purchases logged yet.</td></tr>`;
          return;
        }

        tableBody.innerHTML = data.data.map(p => `
          <tr>
            <td><strong>${p.productTitle}</strong></td>
            <td>${p.userEmail}</td>
            <td>${new Date(p.createdAt).toLocaleString()}</td>
            <td><span class="badge-published">${p.status.toUpperCase()}</span></td>
            <td><span style="font-size:0.75rem;background:#fef3c7;color:#92400e;padding:0.2rem 0.5rem;border-radius:4px;font-weight:700;">Demo Grant</span></td>
          </tr>
        `).join('');
      }
    } catch (err) {
      console.error('Error loading purchases audit:', err);
    }
  }
};
