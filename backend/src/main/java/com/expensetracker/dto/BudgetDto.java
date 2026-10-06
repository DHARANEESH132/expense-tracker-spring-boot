package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public class BudgetDto {

    public static class BudgetRequest {
        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        private Integer month;

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year cannot exceed 2100")
        private Integer year;

        @NotNull(message = "Budget amount is required")
        @Positive(message = "Budget amount must be greater than 0")
        private Double amount;

        public BudgetRequest() {}

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    public static class BudgetResponse {
        private String id;
        private String category;
        private Integer month;
        private Integer year;
        private Double amount;
        private Double spent;
        private Double remaining;
        private Double percentageUsed;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;

        public BudgetResponse() {}

        public BudgetResponse(String id, String category, Integer month, Integer year, Double amount,
                              Double spent, Double remaining, Double percentageUsed, String status,
                              Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.category = category;
            this.month = month;
            this.year = year;
            this.amount = amount;
            this.spent = spent;
            this.remaining = remaining;
            this.percentageUsed = percentageUsed;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @JsonProperty("_id")
        public String getMongoId() { return id; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public Double getSpent() { return spent; }
        public void setSpent(Double spent) { this.spent = spent; }
        public Double getRemaining() { return remaining; }
        public void setRemaining(Double remaining) { this.remaining = remaining; }
        public Double getPercentageUsed() { return percentageUsed; }
        public void setPercentageUsed(Double percentageUsed) { this.percentageUsed = percentageUsed; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}
