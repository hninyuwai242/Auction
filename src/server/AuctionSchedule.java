package server;

import java.io.Serializable;
import java.util.Date;

public class AuctionSchedule implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int id;
    private int itemId;
    private Date scheduledStartTime;
    private Date scheduledEndTime;
    private String status;
    private String itemName;
    private double startingPrice;
    
    public AuctionSchedule() {}
    
    public AuctionSchedule(int id, int itemId, String itemName, double startingPrice, 
                           Date startTime, Date endTime, String status) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.scheduledStartTime = startTime;
        this.scheduledEndTime = endTime;
        this.status = status;
    }
    
    public int getId() { return id; }
    public int getItemId() { return itemId; }
    public Date getScheduledStartTime() { return scheduledStartTime; }
    public Date getScheduledEndTime() { return scheduledEndTime; }
    public String getStatus() { return status; }
    public String getItemName() { return itemName; }
    public double getStartingPrice() { return startingPrice; }
    
    public void setId(int id) { this.id = id; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public void setScheduledStartTime(Date scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }
    public void setScheduledEndTime(Date scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }
    public void setStatus(String status) { this.status = status; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    
    public long getRemainingMillis() {
        if (scheduledStartTime == null) return 0;
        long remaining = scheduledStartTime.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
    
    public String getFormattedRemainingTime() {
        long millis = getRemainingMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    @Override
    public String toString() {
        return itemName + " - " + scheduledStartTime;
    }
}