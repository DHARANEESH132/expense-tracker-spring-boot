/**
 * Authentication Management Module
 */

const auth = {
  getToken() {
    return localStorage.getItem('token');
  },

  getUser() {
    const raw = localStorage.getItem('user');
    try {
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },

  setSession(token, user) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
  },

  clearSession() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  isAuthenticated() {
    return !!this.getToken();
  },

  /**
   * Guard for protected pages. Redirects to login if unauthenticated.
   */
  requireAuth() {
    if (!this.isAuthenticated()) {
      window.location.href = 'login.html';
      return false;
    }
    this.populateUserUI();
    this.setupSidebarToggle();
    return true;
  },

  /**
   * Guard for guest pages (login/register). Redirects to dashboard if already logged in.
   */
  redirectIfAuthenticated() {
    if (this.isAuthenticated()) {
      window.location.href = 'dashboard.html';
      return true;
    }
    return false;
  },

  /**
   * Populate user details in sidebar footer and avatar
   */
  populateUserUI() {
    const user = this.getUser();
    if (!user) return;

    const nameEls = document.querySelectorAll('.user-info-name');
    const emailEls = document.querySelectorAll('.user-info-email');
    const avatarEls = document.querySelectorAll('.avatar-circle');

    nameEls.forEach(el => el.textContent = user.name || 'User');
    emailEls.forEach(el => el.textContent = user.email || '');

    const initial = (user.name && user.name.charAt(0)) ? user.name.charAt(0).toUpperCase() : 'U';
    avatarEls.forEach(el => el.textContent = initial);

    // Setup logout buttons
    const logoutBtns = document.querySelectorAll('.btn-logout-action');
    logoutBtns.forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        this.logout();
      });
    });
  },

  /**
   * Sidebar mobile toggle logic
   */
  setupSidebarToggle() {
    const toggleBtn = document.querySelector('.mobile-menu-btn');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
      toggleBtn.addEventListener('click', () => {
        sidebar.classList.toggle('open');
      });

      // Close when clicking outside on mobile
      document.addEventListener('click', (e) => {
        if (sidebar.classList.contains('open') && !sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
          sidebar.classList.remove('open');
        }
      });
    }
  },

  /**
   * Logout user and clear tokens
   */
  async logout() {
    try {
      await api.post('/auth/logout');
    } catch (e) {
      // Even if network fails, proceed with local logout
    }
    this.clearSession();
    window.location.href = 'login.html?logout=true';
  }
};

// Global toggle password visibility
function setupPasswordToggles() {
  document.querySelectorAll('.toggle-password').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = btn.previousElementSibling;
      if (input && input.type === 'password') {
        input.type = 'text';
        btn.textContent = '👁️';
      } else if (input) {
        input.type = 'password';
        btn.textContent = '👁️‍🗨️';
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  setupPasswordToggles();
});
