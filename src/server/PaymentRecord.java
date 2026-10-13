package server;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private int itemId;
    private String itemName;
    private String winnerName;
    private double amount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private Date paymentDate;
    private String confirmedBy;
    private Date confirmedAt;
    private String notes;
    private Date createdAt;
    
    public PaymentRecord() {
        this.status = "pending";
        this.paymentMethod = null;
        this.transactionId = null;
        this.createdAt = new Date();
        this.paymentDate = null;  // Not paid yet
    }
    
    public PaymentRecord(int id, int itemId, String itemName, String winnerName, 
                         double amount, String status) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.winnerName = winnerName;
        this.amount = amount;
        this.status = status != null ? status : "pending";
        this.paymentMethod = null;
        this.transactionId = null;
        this.createdAt = new Date();
        this.paymentDate = null;  // Not paid yet
    }
    
    // ============ GETTERS ============
    public int getId() { return id; }
    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public String getWinnerName() { return winnerName; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getTransactionId() { return transactionId; }
    public Date getPaymentDate() { return paymentDate; }
    public String getConfirmedBy() { return confirmedBy; }
    public Date getConfirmedAt() { return confirmedAt; }
    public String getNotes() { return notes; }
    public Date getCreatedAt() { return createdAt; }
    
    // ============ SETTERS ============
    public void setId(int id) { this.id = id; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentMethod(String paymentMethod) { 
        this.paymentMethod = (paymentMethod != null && !paymentMethod.isEmpty()) ? paymentMethod : null;
    }
    public void setTransactionId(String transactionId) { 
        this.transactionId = (transactionId != null && !transactionId.isEmpty()) ? transactionId : null;
    }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    public void setConfirmedAt(Date confirmedAt) { this.confirmedAt = confirmedAt; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    // ============ FORMATTED DATES ============
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createdAt);
    }
    
    public String getFormattedPaymentDate() {
        if (paymentDate == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(paymentDate);
    }
    
    public String getFormattedConfirmedAt() {
        if (confirmedAt == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(confirmedAt);
    }
    
    public String getStatusIcon() {
        if (status == null) return "❓";
        switch (status.toLowerCase()) {
            case "pending": return "⏳";
            case "processing": return "🔄";
            case "completed": return "✅";
            case "failed": return "❌";
            case "refunded": return "↩️";
            default: return "❓";
        }
    }
    
    public String getStatusDisplay() {
        if (status == null) return "Pending";
        switch (status.toLowerCase()) {
            case "pending": return "Pending";
            case "processing": return "Processing";
            case "completed": return "Completed";
            case "failed": return "Failed";
            case "refunded": return "Refunded";
            default: return status;
        }
    }
    
    public String getStatusWithIcon() {
        return getStatusIcon() + " " + getStatusDisplay();
    }
    
    @Override
    public String toString() {
        return "PaymentRecord{id=" + id + ", itemName='" + itemName + 
               "', winnerName='" + winnerName + "', amount=" + amount + 
               ", status='" + status + "', createdAt=" + createdAt + "}";
    }
}