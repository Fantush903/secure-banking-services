package com.banking.OnlineBankingWeb.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InsightService {

    /**
     * Returns a mock spending breakdown by category.
     */
    public Map<String, Double> getSpendingBreakdown(Long customerId) {
        Map<String, Double> breakdown = new LinkedHashMap<>();
        breakdown.put("Food", 8500.0);
        breakdown.put("Transport", 3200.0);
        breakdown.put("Shopping", 6800.0);
        breakdown.put("Bills", 12500.0);
        breakdown.put("Entertainment", 2400.0);
        breakdown.put("Healthcare", 1800.0);
        breakdown.put("Education", 4500.0);
        breakdown.put("Others", 3300.0);
        return breakdown;
    }

    /**
     * Returns mock monthly spending trend for the last 6 months.
     */
    public List<Map<String, Object>> getMonthlyTrend(Long customerId) {
        List<Map<String, Object>> trend = new ArrayList<>();

        String[] months = {"January", "February", "March", "April", "May", "June"};
        double[] amounts = {32000.0, 28500.0, 35200.0, 30100.0, 41000.0, 37500.0};

        for (int i = 0; i < months.length; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", months[i]);
            entry.put("amount", amounts[i]);
            trend.add(entry);
        }

        return trend;
    }

    /**
     * Returns mock smart budgeting recommendations.
     */
    public List<String> getBudgetRecommendations(Long customerId) {
        List<String> tips = new ArrayList<>();
        tips.add("Consider reducing dining-out expenses by 15% — cooking at home can save ₹1,200/month.");
        tips.add("Your entertainment spending is within budget — great job keeping it under control!");
        tips.add("Set up auto-transfer of ₹5,000/month to a Fixed Deposit for guaranteed returns.");
        tips.add("Switch to annual subscriptions instead of monthly — you could save up to ₹2,400/year.");
        tips.add("Your bills category is the highest — review recurring subscriptions for unused services.");
        return tips;
    }

    /**
     * Returns a mock savings score (0-100).
     */
    public int getSavingsScore(Long customerId) {
        return 72;
    }
}
