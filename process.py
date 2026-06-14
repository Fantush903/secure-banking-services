import os
import re

files = [
    "card.html",
    "loans.html",
    "fd.html",
    "rd.html",
    "cheque.html",
    "statement.html",
    "standing-instructions.html",
    "currency.html"
]

base_dir = "c:/Users/fantu/Downloads/OnlineBankingWeb/OnlineBankingWeb/src/main/resources/templates"

sidebar_html = """
<!-- ══ SIDEBAR ══ -->
<div class="premium-sidebar" id="sidebar">
    <div class="sidebar-logo">
        <div class="logo-icon">🏦</div>
        <span class="logo-text">SecureBank</span>
    </div>
    <div class="sidebar-user-card">
        <div class="user-avatar" th:text="${session.user.name != null ? session.user.name.substring(0,1) : 'U'}">U</div>
        <div class="user-name" th:text="${session.user.name}">User</div>
        <div class="user-email" th:text="${session.user.email}">user@email.com</div>
        <span class="user-badge">● ACTIVE</span>
    </div>
    <nav>
        <div class="nav-section-label">Main Menu</div>
        <a href="/customer/dashboard" class="premium-nav-item"><span class="nav-icon">📊</span> Dashboard</a>
        <a href="/customer/transfer" class="premium-nav-item"><span class="nav-icon">💸</span> Fund Transfer</a>
        <a href="/customer/transactions" class="premium-nav-item"><span class="nav-icon">📋</span> Transactions</a>
        <a href="/customer/upi" class="premium-nav-item"><span class="nav-icon">📲</span> UPI Payments</a>
        <a href="/customer/loans" class="premium-nav-item ID_loans"><span class="nav-icon">🏠</span> Loans</a>
        <a href="/customer/bills" class="premium-nav-item"><span class="nav-icon">🧾</span> Pay Bills</a>

        <div class="nav-section-label">Account</div>
        <a href="/customer/beneficiaries" class="premium-nav-item"><span class="nav-icon">👥</span> Beneficiaries</a>
        <a href="/customer/card" class="premium-nav-item ID_card"><span class="nav-icon">💳</span> Cards</a>
        <a href="/customer/statement" class="premium-nav-item ID_statement"><span class="nav-icon">📄</span> Statement</a>
        <a href="/customer/fd" class="premium-nav-item ID_fd"><span class="nav-icon">🏦</span> Fixed Deposits</a>
        <a href="/customer/rd" class="premium-nav-item ID_rd"><span class="nav-icon">💰</span> Recurring Deposits</a>
        <a href="/customer/cheque" class="premium-nav-item ID_cheque"><span class="nav-icon">📝</span> Cheque Book</a>
        <a href="/customer/standing-instructions" class="premium-nav-item ID_standing-instructions"><span class="nav-icon">🔄</span> Standing Orders</a>

        <div class="nav-section-label">Services</div>
        <a href="/customer/currency" class="premium-nav-item ID_currency"><span class="nav-icon">💱</span> Currency</a>
        <a href="/customer/nominee" class="premium-nav-item"><span class="nav-icon">👤</span> Nominee</a>
        <a href="/customer/kyc" class="premium-nav-item"><span class="nav-icon">🪪</span> KYC</a>
        <a href="/customer/insights" class="premium-nav-item"><span class="nav-icon">🧠</span> Insights</a>
        <a href="/customer/notifications" class="premium-nav-item"><span class="nav-icon">🔔</span> Notifications</a>

        <div class="nav-section-label">Support</div>
        <a href="/customer/complaints" class="premium-nav-item"><span class="nav-icon">📩</span> Complaints</a>
        <a href="/customer/service-request" class="premium-nav-item"><span class="nav-icon">🛎️</span> Service Request</a>
        <a href="/customer/help" class="premium-nav-item"><span class="nav-icon">❓</span> Help</a>
        <a href="/customer/profile" class="premium-nav-item"><span class="nav-icon">⚙️</span> Profile</a>
    </nav>
    <div class="sidebar-bottom">
        <a href="/logout" class="btn-logout">🚪 Sign Out</a>
    </div>
</div>
"""

topbar_html = """
    <!-- Top Bar -->
    <div class="premium-topbar">
        <button class="mobile-menu-btn" onclick="toggleSidebar()">☰</button>
        <span class="topbar-greeting">Good day, <span th:text="${session.user.name}">User</span> 👋</span>
        <div class="topbar-actions">
            <button class="topbar-btn" onclick="toggleDarkMode()" title="Toggle Theme">🌙</button>
            <span class="topbar-clock" id="clock"></span>
            <a href="/customer/notifications" class="topbar-btn" title="Notifications">
                🔔<div class="notification-badge"></div>
            </a>
        </div>
    </div>
"""

def map_classes(html_content):
    html_content = re.sub(r'class="alert alert-success"', 'class="premium-alert premium-alert-success"', html_content)
    html_content = re.sub(r'class="alert alert-error"', 'class="premium-alert premium-alert-error"', html_content)
    html_content = re.sub(r'class="alert alert-warning"', 'class="premium-alert premium-alert-warning"', html_content)
    html_content = re.sub(r'<input\b(?![^>]*class=")', '<input class="premium-input" ', html_content)
    html_content = re.sub(r'<select\b(?![^>]*class=")', '<select class="premium-select" ', html_content)
    html_content = re.sub(r'<label\b(?![^>]*class=")', '<label class="premium-label" ', html_content)
    html_content = re.sub(r'class="([^"]*)\bbtn\b([^"]*)"', lambda m: f'class="{m.group(1)}premium-btn{m.group(2)}"', html_content)
    html_content = re.sub(r'class="([^"]*)\bcard\b([^"]*)"', lambda m: f'class="{m.group(1)}premium-card{m.group(2)}"', html_content)
    html_content = re.sub(r'<table\b(?![^>]*class=")', '<table class="premium-table" ', html_content)
    html_content = re.sub(r'<table class="([^"]*)"', lambda m: '<table class="premium-table ' + m.group(1).replace('premium-table', '').strip() + '"', html_content)

    return html_content

for fname in files:
    path = os.path.join(base_dir, fname)
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Extract content inside <div class="main"> or similar
    # Sometimes it's <div class="content"> or just follows topbar
    # Let's write a heuristic
    main_match = re.search(r'<div class="main">(.*?)</div>\s*<script', content, re.DOTALL)
    if not main_match:
        main_match = re.search(r'<div class="main">(.*?)</script>', content, re.DOTALL)
        
    script_part = ""
    script_match = re.search(r'(<script>.*?</script>)', content, re.DOTALL)
    if script_match:
        script_part = script_match.group(1)

    # For card.html there's <div class="content"> inside <div class="main">
    
    print(f"{fname}: main_match found? {bool(main_match)}")
