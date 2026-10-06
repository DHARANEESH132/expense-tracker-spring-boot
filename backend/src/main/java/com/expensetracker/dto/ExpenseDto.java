package com.expensetracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseDto {

    public static class ExpenseRequest {
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be a positive number greater than 0")
        private Double amount;

        @NotBlank(message = "Category is required")
        private String category;

        @NotBlank(message = "Description is required")
        @Size(max = 200, message = "Description cannot exceed 200 characters")
        private String description;

        @NotBlank(message = "Payment method is required")
        private String paymentMethod;

        private LocalDate date;

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        private String notes;

        public ExpenseRequest() {}

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class ExpenseResponse {
        private String id;
        private Double amount;
        private String category;
        private String description;
        private String paymentMethod;
        private LocalDate date;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;

        public ExpenseResponse() {}

        public ExpenseResponse(String id, Double amount, String category, String description,
                               String paymentMethod, LocalDate date, String notes,
                               Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.amount = amount;
            this.category = category;
            this.description = description;
            this.paymentMethod = paymentMethod;
            this.date = date;
            this.notes = notes;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @JsonProperty("_id")
        public String getMongoId() { return id; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }
}
