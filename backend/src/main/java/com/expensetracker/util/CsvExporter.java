package com.expensetracker.util;

import com.expensetracker.dto.ExpenseDto.ExpenseResponse;

import java.util.List;

public class CsvExporter {

    private static String escapeCsv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return "\"" + str + "\"";
    }

    public static String exportToCsv(List<ExpenseResponse> expenses) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Date,Category,Description,Amount,Payment Method,Notes,Created At\r\n");

        for (ExpenseResponse exp : expenses) {
            sb.append(escapeCsv(exp.getId())).append(",")
              .append(escapeCsv(exp.getDate())).append(",")
              .append(escapeCsv(exp.getCategory())).append(",")
              .append(escapeCsv(exp.getDescription())).append(",")
              .append(escapeCsv(exp.getAmount())).append(",")
              .append(escapeCsv(exp.getPaymentMethod())).append(",")
              .append(escapeCsv(exp.getNotes())).append(",")
              .append(escapeCsv(exp.getCreatedAt())).append("\r\n");
        }

        return sb.toString();
    }
}
