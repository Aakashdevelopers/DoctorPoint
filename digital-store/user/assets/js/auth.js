// User Panel Auth Manager with Live Firebase ID Token Support

const Auth = {
  // Returns fresh Firebase ID token or dev token
  async getToken(forceRefresh = false) {
    if (typeof firebase !== 'undefined' && firebase.auth && firebase.auth().currentUser) {
      try {
        return await firebase.auth().currentUser.getIdToken(forceRefresh);
      } catch (e) {
        console.warn('Firebase ID token error:', e);
      }
    }

    const devToken = localStorage.getItem('novastore_dev_token');
    if (devToken) return devToken;

    return null;
  },

  getUser() {
    const userJson = localStorage.getItem('novastore_user');
    return userJson ? JSON.parse(userJson) : null;
  },

  async login(email, password) {
    if (typeof firebase !== 'undefined' && firebase.auth) {
      try {
        const userCred = await firebase.auth().signInWithEmailAndPassword(email, password);
        const token = await userCred.user.getIdToken();

        const res = await fetch('/api/auth/sync', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ displayName: userCred.user.displayName || email.split('@')[0] })
        });
        const data = await res.json();

        if (data.success) {
          localStorage.setItem('novastore_user', JSON.stringify(data.data));
          return { success: true, user: data.data };
        }
      } catch (err) {
        console.warn('[Firebase Auth] Sign in error:', err.message);
        if (window.location.protocol === 'file:') {
          // Dev Fallback mode strictly for local file testing
          return this.devModeFallback(email);
        }
        return { success: false, error: err.message };
      }
    }

    return this.devModeFallback(email);
  },

  async register(email, password, displayName) {
    if (typeof firebase !== 'undefined' && firebase.auth) {
      try {
        const userCred = await firebase.auth().createUserWithEmailAndPassword(email, password);
        if (displayName) {
          await userCred.user.updateProfile({ displayName });
        }
        const token = await userCred.user.getIdToken();

        const res = await fetch('/api/auth/sync', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ displayName: displayName || email.split('@')[0] })
        });
        const data = await res.json();
        if (data.success) {
          localStorage.setItem('novastore_user', JSON.stringify(data.data));
          return { success: true, user: data.data };
        }
      } catch (err) {
        console.warn('[Firebase Auth] Register error:', err.message);
        return { success: false, error: err.message };
      }
    }

    return this.devModeFallback(email);
  },

  devModeFallback(email) {
    const isAdmin = email.toLowerCase().includes('admin');
    const devToken = `dev-token-${Date.now()}:${email.toLowerCase()}:${isAdmin ? 'Admin User' : 'Customer'}`;
    const userObj = {
      uid: `dev-uid-${email.toLowerCase().replace(/[^a-z0-9]/g, '')}`,
      email: email.toLowerCase(),
      displayName: isAdmin ? 'System Admin' : 'Customer',
      role: isAdmin ? 'admin' : 'user',
      isAdmin: isAdmin
    };

    localStorage.setItem('novastore_dev_token', devToken);
    localStorage.setItem('novastore_user', JSON.stringify(userObj));

    return { success: true, user: userObj };
  },

  async logout() {
    if (typeof firebase !== 'undefined' && firebase.auth) {
      try {
        await firebase.auth().signOut();
      } catch (e) {}
    }
    localStorage.removeItem('novastore_dev_token');
    localStorage.removeItem('novastore_user');
    window.location.href = 'login.html';
  },

  updateNavUI() {
    const user = this.getUser();
    const navLinksContainer = document.getElementById('nav-links-dynamic');
    if (!navLinksContainer) return;

    const isSubdir = window.location.pathname.includes('/admin/');
    const homePath = isSubdir ? '../index.html' : 'index.html';
    const myPurchasesPath = isSubdir ? '../my-purchases.html' : 'my-purchases.html';
    const adminPath = isSubdir ? 'index.html' : 'admin/index.html';
    const loginPath = isSubdir ? '../login.html' : 'login.html';

    if (user) {
      navLinksContainer.innerHTML = `
        <a href="${homePath}" class="nav-link">Store</a>
        <a href="${myPurchasesPath}" class="nav-link">My Purchases</a>
        ${user.isAdmin ? `<a href="${adminPath}" class="nav-link" style="color:#6366f1;font-weight:700;">Admin Panel</a>` : ''}
        <span style="font-size:0.88rem;color:var(--text-muted);margin-left:0.5rem;">Hi, <strong>${user.displayName || user.email.split('@')[0]}</strong></span>
        <button onclick="Auth.logout()" class="btn btn-outline" style="padding:0.4rem 0.9rem;font-size:0.85rem;">Logout</button>
      `;
    } else {
      navLinksContainer.innerHTML = `
        <a href="${homePath}" class="nav-link">Store</a>
        <a href="${loginPath}" class="btn btn-outline" style="padding:0.4rem 1rem;">Sign In</a>
        <a href="${loginPath}?mode=signup" class="btn btn-primary" style="padding:0.4rem 1rem;">Sign Up</a>
      `;
    }
  }
};

// Initialize Firebase Auth State Listener
document.addEventListener('DOMContentLoaded', () => {
  Auth.updateNavUI();

  if (typeof firebase !== 'undefined' && firebase.auth) {
    firebase.auth().onAuthStateChanged(async (fbUser) => {
      if (fbUser) {
        try {
          const token = await fbUser.getIdToken();
          const res = await fetch('/api/auth/sync', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ displayName: fbUser.displayName || fbUser.email.split('@')[0] })
          });
          const data = await res.json();
          if (data.success) {
            localStorage.setItem('novastore_user', JSON.stringify(data.data));
            Auth.updateNavUI();
          }
        } catch (e) {
          console.warn('[Firebase Auth Listener] Sync warning:', e);
        }
      }
    });
  }
});
