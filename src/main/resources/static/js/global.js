// ═══════════════════════════════════════════════════════
// SecureBank — Global JavaScript
// Dark Mode + Mobile Menu + Animations + WebSockets + Utilities
// Include in all pages: <script th:src="@{/js/global.js}"></script>
// ═══════════════════════════════════════════════════════

// ── Dark Mode System-Preference Aware Initialization ───
(function() {
    const saved = localStorage.getItem('darkMode');
    if (saved === 'true' || (saved === null && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.setAttribute('data-theme', 'light');
    }
})();

// Helper to load external scripts dynamically (for WebSockets / STOMP)
function loadScript(url, callback) {
    if (document.querySelector(`script[src="${url}"]`)) {
        if (callback) callback();
        return;
    }
    const script = document.createElement('script');
    script.src = url;
    script.onload = callback;
    document.head.appendChild(script);
}

// ── WebSocket Connection Setup ─────────────────────────
function initWebSocket(customerId) {
    // Load SockJS & STOMP from CDN dynamically
    loadScript('https://cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.6.1/sockjs.min.js', function() {
        loadScript('https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js', function() {
            try {
                const socket = new SockJS('/ws');
                const stompClient = Stomp.over(socket);
                stompClient.debug = null; // Disable debug logs in console
                
                stompClient.connect({}, function (frame) {
                    stompClient.subscribe('/topic/notifications/' + customerId, function (message) {
                        const notif = JSON.parse(message.body);
                        showToast(notif.title, notif.message, notif.type);
                        updateNotificationCount(1);
                    });
                }, function (error) {
                    console.log('STOMP connection lost, retrying in 5s...', error);
                    setTimeout(() => initWebSocket(customerId), 5000);
                });
            } catch (e) {
                console.error('WebSocket initialization failed:', e);
            }
        });
    });
}

// ── Notification Badge Updater ─────────────────────────
function updateNotificationCount(increment) {
    const badge = document.querySelector('.notification-badge');
    if (badge) {
        let current = parseInt(badge.textContent) || 0;
        current += increment;
        badge.textContent = current;
        badge.style.display = current > 0 ? 'flex' : 'none';
        
        // Add a micro-pulse animation to topbar icon
        const parent = badge.parentElement;
        if (parent) {
            parent.style.animation = 'pulse 0.3s ease-out';
            setTimeout(() => parent.style.animation = '', 300);
        }
    }
}

// ── Premium Toast Notification Renderer ────────────────
function showToast(title, message, type = 'INFO') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position:fixed; bottom:24px; right:24px; z-index:9999; display:flex; flex-direction:column; gap:12px; max-width:380px; width:100%;';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'glass-card';
    
    // Choose icon/color based on type
    let emoji = '🔔';
    let border = '#6366f1'; // primary
    if (type === 'ALERT' || type === 'SECURITY') { emoji = '⚠️'; border = '#ef4444'; }
    else if (type === 'TRANSACTION') { emoji = '💸'; border = '#10b981'; }

    toast.style.cssText = `
        background: var(--glass-bg);
        border: 1px solid var(--glass-border);
        border-left: 4px solid ${border};
        backdrop-filter: blur(16px);
        padding: 16px;
        border-radius: 12px;
        box-shadow: var(--glass-shadow);
        display: flex;
        gap: 12px;
        align-items: flex-start;
        opacity: 0;
        transform: translateY(20px);
        transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    `;

    toast.innerHTML = `
        <div style="font-size: 20px;">${emoji}</div>
        <div style="flex: 1;">
            <div style="font-weight: 600; font-size: 14px; color: var(--text-white); margin-bottom: 2px;">${title}</div>
            <div style="font-size: 12px; color: var(--text-muted); line-height: 1.4;">${message}</div>
        </div>
        <button onclick="this.parentElement.remove()" style="background:none; border:none; color:var(--text-dim); cursor:pointer; font-size:16px;">×</button>
    `;

    container.appendChild(toast);
    
    // Animate in
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
    }, 100);

    // Auto remove after 6 seconds
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(20px)';
        setTimeout(() => toast.remove(), 400);
    }, 6000);
}

// ── Dark Mode Toggler ──────────────────────────────────
function toggleDarkMode() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const newMode = isDark ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newMode);
    localStorage.setItem('darkMode', !isDark);
    const btn = document.querySelector('.dark-mode-toggle');
    if (btn) btn.textContent = isDark ? '🌙' : '☀️';
}

// ── DOMContentLoaded Listener ──────────────────────────
document.addEventListener('DOMContentLoaded', function() {
    // Sync dark mode toggle button icon
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const btn = document.querySelector('.dark-mode-toggle');
    if (btn) btn.textContent = isDark ? '☀️' : '🌙';

    // ── Mobile Menu Toggling ─────────────────────────
    const menuBtn = document.querySelector('.mobile-menu-btn');
    const sidebar = document.querySelector('.premium-sidebar') || document.querySelector('.sidebar');
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

    // ── Auto-dismiss Alerts ──────────────────────────
    const alerts = document.querySelectorAll('.premium-alert, .alert-success, .alert-error');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s, transform 0.5s';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 6000);
    });

    // ── Active Nav Highlight ─────────────────────────
    const currentPath = window.location.pathname;
    document.querySelectorAll('.premium-nav-item, .nav-item').forEach(item => {
        if (item.getAttribute('href') === currentPath) {
            item.classList.add('active');
        }
    });

    // ── Live Clock ───────────────────────────────────
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

    // ── Staggered Scroll Animations (IntersectionObserver) ──
    if ('IntersectionObserver' in window) {
        const animationObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                    animationObserver.unobserve(entry.target);
                }
            });
        }, { threshold: 0.05 });

        document.querySelectorAll('.animate-in, .glass-card, .premium-stat-card').forEach((el, index) => {
            // Apply a staggered delay dynamically if not already present
            if (!el.style.animationDelay) {
                el.style.animationDelay = `${index * 0.05}s`;
            }
            animationObserver.observe(el);
        });
    }

    // ── Smooth Scroll Behavior ───────────────────────
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;
            const targetEl = document.querySelector(targetId);
            if (targetEl) {
                e.preventDefault();
                targetEl.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // ── Fetch active customer notification count & start WebSockets ──
    if (window.location.pathname.startsWith('/customer')) {
        fetch('/customer/notifications/unread-count')
            .then(res => res.json())
            .then(data => {
                if (data && data.customerId) {
                    // Update initial topbar badge count
                    const badge = document.querySelector('.notification-badge');
                    if (badge) {
                        badge.textContent = data.unreadCount;
                        badge.style.display = data.unreadCount > 0 ? 'flex' : 'none';
                    }
                    
                    // Connect to WebSocket using the customerId
                    initWebSocket(data.customerId);
                }
            })
            .catch(err => console.log('Error initializing notification system:', err));
    }
});

// ── OTP Countdown Timer Helper ────────────────────────
function startOtpCountdown(durationSeconds, displayElId, buttonElId, textElId) {
    let seconds = durationSeconds;
    const displayEl = document.getElementById(displayElId);
    const buttonEl = document.getElementById(buttonElId);
    const textEl = document.getElementById(textElId);
    
    if (!displayEl) return;
    
    displayEl.textContent = seconds;
    const timer = setInterval(() => {
        seconds--;
        displayEl.textContent = seconds;
        if (seconds <= 0) {
            clearInterval(timer);
            if (textEl) textEl.style.display = 'none';
            if (buttonEl) buttonEl.classList.remove('disabled');
        }
    }, 1000);
    return timer;
}

// ── Format Currency ───────────────────────────────────
function formatCurrency(amount) {
    return '₹ ' + parseFloat(amount).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

// ── Confirm Before Action ─────────────────────────────
function confirmAction(message, url) {
    if (confirm(message)) window.location.href = url;
}

// ── Copy to Clipboard ─────────────────────────────────
function copyText(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('Copied', 'Text copied to clipboard successfully!', 'INFO');
    });
}