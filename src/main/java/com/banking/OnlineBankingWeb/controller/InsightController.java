package com.banking.OnlineBankingWeb.controller;

import com.banking.OnlineBankingWeb.model.Customer;
import com.banking.OnlineBankingWeb.service.InsightService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class InsightController {

    @Autowired
    private InsightService insightService;

    @GetMapping("/customer/insights")
    public String insights(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) return "redirect:/login";
        Customer user = (Customer) session.getAttribute("user");
        long customerId = (long) user.getCustomerID();

        Map<String, Double> spendingBreakdown = insightService.getSpendingBreakdown(customerId);
        List<Map<String, Object>> monthlyTrend = insightService.getMonthlyTrend(customerId);
        List<String> recommendations = insightService.getBudgetRecommendations(customerId);
        int savingsScore = insightService.getSavingsScore(customerId);

        model.addAttribute("spendingBreakdown", spendingBreakdown);
        model.addAttribute("monthlyTrend", monthlyTrend);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("savingsScore", savingsScore);

        return "insights";
    }
}
