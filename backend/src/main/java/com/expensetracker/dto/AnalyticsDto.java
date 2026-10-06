package com.expensetracker.dto;

import java.util.List;

public class AnalyticsDto {

    public static class ExpenseSummaryResponse {
        private Double totalExpenses;
        private Long expenseCount;
        private Double averageExpense;
        private Double highestExpense;
        private Double lowestExpense;

        public ExpenseSummaryResponse() {}

        public ExpenseSummaryResponse(Double totalExpenses, Long expenseCount, Double averageExpense,
                                      Double highestExpense, Double lowestExpense) {
            this.totalExpenses = totalExpenses;
            this.expenseCount = expenseCount;
            this.averageExpense = averageExpense;
            this.highestExpense = highestExpense;
            this.lowestExpense = lowestExpense;
        }

        public Double getTotalExpenses() { return totalExpenses; }
        public void setTotalExpenses(Double totalExpenses) { this.totalExpenses = totalExpenses; }
        public Long getExpenseCount() { return expenseCount; }
        public void setExpenseCount(Long expenseCount) { this.expenseCount = expenseCount; }
        public Double getAverageExpense() { return averageExpense; }
        public void setAverageExpense(Double averageExpense) { this.averageExpense = averageExpense; }
        public Double getHighestExpense() { return highestExpense; }
        public void setHighestExpense(Double highestExpense) { this.highestExpense = highestExpense; }
        public Double getLowestExpense() { return lowestExpense; }
        public void setLowestExpense(Double lowestExpense) { this.lowestExpense = lowestExpense; }
    }

    public static class CategorySummaryItem {
        private String category;
        private Double total;
        private Long count;
        private Double percentage;

        public CategorySummaryItem() {}

        public CategorySummaryItem(String category, Double total, Long count, Double percentage) {
            this.category = category;
            this.total = total;
            this.count = count;
            this.percentage = percentage;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    public static class CategorySummaryResponse {
        private Double grandTotal;
        private List<CategorySummaryItem> categories;

        public CategorySummaryResponse() {}

        public CategorySummaryResponse(Double grandTotal, List<CategorySummaryItem> categories) {
            this.grandTotal = grandTotal;
            this.categories = categories;
        }

        public Double getGrandTotal() { return grandTotal; }
        public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
        public List<CategorySummaryItem> getCategories() { return categories; }
        public void setCategories(List<CategorySummaryItem> categories) { this.categories = categories; }
    }

    public static class MonthlySummaryItem {
        private Integer year;
        private Integer month;
        private String label;
        private Double total;
        private Long count;

        public MonthlySummaryItem() {}

        public MonthlySummaryItem(Integer year, Integer month, String label, Double total, Long count) {
            this.year = year;
            this.month = month;
            this.label = label;
            this.total = total;
            this.count = count;
        }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }
}
