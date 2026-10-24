// Admin Panel Controller - Database Only Mode

const Admin = {
  async getAuthHeaders() {
    const token = await Auth.getToken();
    if (!token) return null;
    return { 'Authorization': `Bearer ${token}` };
  },

  async verifyAdminAccess() {
    const token = await Auth.getToken();

    if (!token) {
      this.redirectToLogin();
      return false;
    }

    try {
      const res = await fetch('/api/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          alert('Access denied. Administrator privileges required.');
          this.redirectToLogin();
          return false;
        }
      }

      const data = await res.json();
      if (data.success && data.data && data.data.isAdmin) {
        localStorage.setItem('novastore_user', JSON.stringify(data.data));
        return true;
      } else {
        alert('Access denied. Administrator privileges required.');
        window.location.href = '../user/index.html';
        return false;
      }
    } catch (err) {
      console.error('Could not verify admin status with backend:', err);
      // Fallback verification check using stored user claim
      const user = Auth.getUser();
      if (user && user.isAdmin) {
        return true;
      }
      alert('Authentication error. Access denied.');
      this.redirectToLogin();
      return false;
    }
  },

  redirectToLogin() {
    const isSubdir = window.location.pathname.includes('/admin/');
    const loginPath = isSubdir ? '../user/login.html' : 'user/login.html';
    const redirectTarget = window.location.pathname;
    window.location.href = `${loginPath}?redirect=${encodeURIComponent(redirectTarget)}`;
  },

  async initDashboard() {
    if (!(await this.verifyAdminAccess())) return;
    this.loadStats();
    this.loadProducts();
    this.loadPurchasesAudit();
  },

  async loadStats() {
    const headers = await this.getAuthHeaders();
    if (!headers) return;

    try {
      const res = await fetch('/api/admin/stats', { headers });
      const data = await res.json();

      if (data.success && data.data) {
        const setTxt = (id, val) => {
          const el = document.getElementById(id);
          if (el) el.textContent = val;
        };
        setTxt('stat-total-products', data.data.totalProducts ?? 0);
        setTxt('stat-published-products', data.data.publishedProducts ?? 0);
        setTxt('stat-demo-purchases', data.data.totalDemoPurchases ?? 0);
        setTxt('stat-draft-products', data.data.draftProducts ?? 0);
        return;
      }
    } catch (err) {
      console.error('Error fetching admin stats from database:', err);
    }

    const setTxt = (id, val) => {
      const el = document.getElementById(id);
      if (el) el.textContent = val;
    };
    setTxt('stat-total-products', '0');
    setTxt('stat-published-products', '0');
    setTxt('stat-demo-purchases', '0');
    setTxt('stat-draft-products', '0');
  },

  async loadProducts(search = '', status = 'all') {
    const tableBody = document.getElementById('products-table-body');
    if (!tableBody) return;

    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">Loading products from database...</td></tr>`;

    const headers = await this.getAuthHeaders();
    if (!headers) {
      tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:#ef4444;">Authentication token missing. Please login.</td></tr>`;
      return;
    }

    try {
      const params = new URLSearchParams();
      if (search) params.append('search', search);
      if (status) params.append('status', status);

      const res = await fetch(`/api/admin/products?${params.toString()}`, { headers });
      if (res.status === 401 || res.status === 403) {
        tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:#ef4444;">Access denied. Administrator privileges required.</td></tr>`;
        return;
      }

      const data = await res.json();
      if (data.success && Array.isArray(data.data)) {
        this.renderProductsTable(data.data);
        return;
      } else {
        tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">${data.error || 'No products found.'}</td></tr>`;
        return;
      }
    } catch (err) {
      console.error('Error fetching admin products from database:', err);
    }

    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:#ef4444;">Could not load products. Please check server database connection.</td></tr>`;
  },

  renderProductsTable(products) {
    const tableBody = document.getElementById('products-table-body');
    if (!tableBody) return;

    if (products.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">No products found in MySQL database matching filter criteria.</td></tr>`;
      return;
    }

    tableBody.innerHTML = products.map(p => `
      <tr>
        <td>
          <div style="display:flex;align-items:center;gap:0.8rem;">
            <img src="${p.coverImageUrl}" alt="" style="width:42px;height:42px;border-radius:6px;object-fit:cover;" onerror="this.src='https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80'">
            <div>
              <strong style="display:block;font-size:0.95rem;">${this.escapeHtml(p.title)}</strong>
              <small style="color:var(--text-muted);">${this.escapeHtml(p.category)}</small>
            </div>
          </div>
        </td>
        <td><strong>₹${p.priceInINR}</strong></td>
        <td><span class="badge-${p.status}">${p.status.toUpperCase()}</span></td>
        <td>${new Date(p.createdAt || Date.now()).toLocaleDateString()}</td>
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
    if (!headers) return;
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
        this.loadProducts(
          document.getElementById('product-search')?.value || '',
          document.getElementById('product-status-filter')?.value || 'all'
        );
        return;
      } else {
        alert(data.error || 'Failed to change product status.');
      }
    } catch (err) {
      console.error('Error changing product status:', err);
      alert('Error connecting to backend database server.');
    }
  },

  async archiveProduct(id) {
    if (!confirm('Are you sure you want to archive this product? Archived products will be hidden from the public store, but historical purchases will be safely preserved.')) {
      return;
    }

    const headers = await this.getAuthHeaders();
    if (!headers) return;

    try {
      const res = await fetch(`/api/admin/products/${id}`, {
        method: 'DELETE',
        headers
      });
      const data = await res.json();
      if (data.success) {
        this.loadStats();
        this.loadProducts(
          document.getElementById('product-search')?.value || '',
          document.getElementById('product-status-filter')?.value || 'all'
        );
        return;
      } else {
        alert(data.error || 'Failed to archive product.');
      }
    } catch (err) {
      console.error('Error archiving product:', err);
      alert('Error connecting to backend database server.');
    }
  },

  async loadPurchasesAudit() {
    const tableBody = document.getElementById('purchases-audit-table-body');
    if (!tableBody) return;

    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">Loading audit log from database...</td></tr>`;

    const headers = await this.getAuthHeaders();
    if (!headers) {
      tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:#ef4444;">Authentication token missing. Please login.</td></tr>`;
      return;
    }

    try {
      const res = await fetch('/api/admin/purchases', { headers });
      if (res.status === 401 || res.status === 403) {
        tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:#ef4444;">Access denied. Administrator privileges required.</td></tr>`;
        return;
      }

      const data = await res.json();

      if (data.success && Array.isArray(data.data)) {
        if (data.data.length === 0) {
          tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">No demo purchases logged in MySQL database yet.</td></tr>`;
          return;
        }

        tableBody.innerHTML = data.data.map(p => `
          <tr>
            <td><strong>${this.escapeHtml(p.productTitle)}</strong></td>
            <td>${this.escapeHtml(p.userEmail)}</td>
            <td>${new Date(p.createdAt).toLocaleString()}</td>
            <td><span class="badge-published">${p.status}</span></td>
            <td><span style="font-size:0.75rem;background:#fef3c7;color:#92400e;padding:0.2rem 0.5rem;border-radius:4px;font-weight:700;">Demo Grant</span></td>
          </tr>
        `).join('');
        return;
      }
    } catch (err) {
      console.error('Error loading purchase audit log:', err);
    }

    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">Could not fetch audit log from database.</td></tr>`;
  },

  escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
};
