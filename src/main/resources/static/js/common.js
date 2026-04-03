// Shared utilities for all pages

// Toast notifications
function showToast(message, type = 'info') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

// Format currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
    }).format(amount);
}

// Format date
function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-IN', {
        year: 'numeric', month: 'short', day: 'numeric'
    });
}

// Initialize sidebar and user info on protected pages
function initPage(activeNav) {
    if (!requireAuth()) return false;

    const user = getUser();
    document.body.classList.add(`role-${user.role}`);

    // Set user info in sidebar
    const nameEl = document.getElementById('user-name');
    const roleEl = document.getElementById('user-role');
    if (nameEl) nameEl.textContent = user.username;
    if (roleEl) roleEl.textContent = user.role;

    // Mark active nav
    const navLinks = document.querySelectorAll('.sidebar-nav a');
    navLinks.forEach(link => {
        if (link.dataset.page === activeNav) {
            link.classList.add('active');
        }
    });

    // Hide nav items based on role
    if (user.role === 'VIEWER') {
        document.querySelectorAll('[data-page="dashboard"]').forEach(el => el.parentElement.style.display = 'none');
        document.querySelectorAll('[data-page="users"]').forEach(el => el.parentElement.style.display = 'none');
    } else if (user.role === 'ANALYST') {
        document.querySelectorAll('[data-page="users"]').forEach(el => el.parentElement.style.display = 'none');
    }

    // Logout
    document.getElementById('btn-logout')?.addEventListener('click', () => {
        clearAuth();
        window.location.href = '/index.html';
    });

    return true;
}

// Sidebar HTML template
function sidebarHTML() {
    return `
    <div class="sidebar">
        <div class="sidebar-brand">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="1" x2="12" y2="23"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
            FinanceApp
        </div>
        <ul class="sidebar-nav">
            <li><a href="/dashboard.html" data-page="dashboard">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                Dashboard
            </a></li>
            <li><a href="/records.html" data-page="records">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line></svg>
                Records
            </a></li>
            <li><a href="/users.html" data-page="users">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                Users
            </a></li>
        </ul>
        <div class="sidebar-user">
            <div class="user-info">
                <span class="user-name" id="user-name">-</span>
                <span class="user-role" id="user-role">-</span>
            </div>
            <button class="btn-logout" id="btn-logout">Logout</button>
        </div>
    </div>`;
}
