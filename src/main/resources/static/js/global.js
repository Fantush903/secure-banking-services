// ═══════════════════════════════════════════════════════
// SecureBank — Global JavaScript
// Dark Mode + Mobile Menu + Utilities
// Include in all pages: <script th:src="@{/js/global.js}"></script>
// ═══════════════════════════════════════════════════════

// ── Dark Mode ─────────────────────────────────────────
(function() {
    const saved = localStorage.getItem('darkMode');
    if (saved === 'true') {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
})();

function toggleDarkMode() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const newMode = isDark ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newMode);
    localStorage.setItem('darkMode', !isDark);
    const btn = document.querySelector('.dark-mode-toggle');
    if (btn) btn.textContent = isDark ? '🌙' : '☀️';
}

// Set correct icon on load
document.addEventListener('DOMContentLoaded', function() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const btn = document.querySelector('.dark-mode-toggle');
    if (btn) btn.textContent = isDark ? '☀️' : '🌙';

    // ── Mobile Menu ──────────────────────────────────
    const menuBtn = document.querySelector('.mobile-menu-btn');
    const sidebar = document.querySelector('.sidebar');

    // Create overlay
    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    if (menuBtn && sidebar) {
        menuBtn.addEventListener('click', function() {
            sidebar.classList.toggle('open');
            overlay.classList.toggle('show');
        });
        overlay.addEventListener('click', function() {
            sidebar.classList.remove('open');
            overlay.classList.remove('show');
        });
    }

    // ── Auto-dismiss alerts ──────────────────────────
    const alerts = document.querySelectorAll('.alert-success, .alert-error');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });

    // ── Active nav highlight ─────────────────────────
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('href') === currentPath) {
            item.classList.add('active');
        }
    });

    // ── Live clock ───────────────────────────────────
    const clockEl = document.getElementById('clock');
    if (clockEl) {
        function updateClock() {
            clockEl.textContent = new Date().toLocaleTimeString('en-IN', {
                hour: '2-digit', minute: '2-digit'
            });
        }
        updateClock();
        setInterval(updateClock, 1000);
    }
});

// ── Format currency ───────────────────────────────────
function formatCurrency(amount) {
    return '₹ ' + parseFloat(amount).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

// ── Confirm before action ─────────────────────────────
function confirmAction(message, url) {
    if (confirm(message)) window.location.href = url;
}

// ── Copy to clipboard ─────────────────────────────────
function copyText(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert('Copied: ' + text);
    });
}