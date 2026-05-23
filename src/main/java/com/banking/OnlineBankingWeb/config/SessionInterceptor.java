package com.banking.OnlineBankingWeb.config;

import com.banking.OnlineBankingWeb.model.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI();

        // ── Public paths — always allowed ──────────────────
        if (path.equals("/") ||
                path.equals("/login") ||
                path.equals("/register") ||
                path.equals("/forgot-password") ||
                path.startsWith("/verify-otp") ||
                path.startsWith("/reset-password") ||
                path.startsWith("/resend-otp") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/error")) {
            return true;
        }

        HttpSession session = request.getSession(false);

        // ── Not logged in → redirect to login ──────────────
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("/login?error=Session+expired.+Please+login+again.");
            return false;
        }

        Customer user = (Customer) session.getAttribute("user");

        // ── Admin trying to access customer pages ───────────
        if (path.startsWith("/customer") && user.getRole().equals("ADMIN")) {
            response.sendRedirect("/admin/dashboard");
            return false;
        }

        // ── Customer trying to access admin pages ───────────
        if (path.startsWith("/admin") && !user.getRole().equals("ADMIN")) {
            response.sendRedirect("/customer/dashboard?error=Access+denied.");
            return false;
        }

        // ── Locked account ──────────────────────────────────
        if (user.getStatus().equals("LOCKED")) {
            session.invalidate();
            response.sendRedirect("/login?error=Your+account+is+locked.+Please+contact+admin.");
            return false;
        }

        return true;
    }
}