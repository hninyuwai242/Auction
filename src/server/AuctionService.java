package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

public interface AuctionService extends Remote {
    
    // ============ ITEM METHODS ============
    
    List<AuctionItem> getItems() throws RemoteException;
    
    boolean placeBid(int itemId, double amount, String bidderName, ClientCallback callback) throws RemoteException;
    
    void addItem(AuctionItem item) throws RemoteException;
    
    void addNewItem(String name, String description, double startingPrice, byte[] imageData) throws RemoteException;
    
    void addNewItemWithImage(String name, String description, double startingPrice, byte[] imageData, String imageFileName) throws RemoteException;
    
    // ============ CLIENT METHODS ============
    
    void registerClient(ClientCallback callback) throws RemoteException;
    
    void unregisterClient(ClientCallback callback) throws RemoteException;
    
    void broadcastChatMessage(String sender, String message) throws RemoteException;
    
    String getServerIP() throws RemoteException;
    
    List<String> getConnectedClients() throws RemoteException;
    
    boolean isItemBiddable(int itemId) throws RemoteException;
    
    AuctionItem getCurrentActiveItem() throws RemoteException;
    
    // ============ SCHEDULE METHODS ============
    
    void enableScheduledMode(boolean enable) throws RemoteException;
    
    void createAuctionSchedule(int itemId, Date startTime) throws RemoteException;
    
    List<AuctionSchedule> getPendingSchedules() throws RemoteException;
    
    void cancelSchedule(int scheduleId) throws RemoteException;
    
    List<String[]> getUpcomingSchedulesInfo() throws RemoteException;
    
    Date getNextAuctionTime() throws RemoteException;
    
    long getNextAuctionCountdown() throws RemoteException;
    
    String[] getNextAuctionDetails() throws RemoteException;
    
    boolean isScheduledModeEnabled() throws RemoteException;
    
    // ============================================================
    // ============ PAYMENT METHODS ============
    // ============================================================
    
    void createPaymentRecord(int itemId, String winnerName, double amount) throws RemoteException;
    
    void updatePaymentStatus(int paymentId, String status, String confirmedBy) throws RemoteException;
    
    void updatePaymentDetails(int paymentId, String paymentMethod, String transactionId, Date paymentDate) throws RemoteException;
    
    PaymentRecord getPaymentById(int paymentId) throws RemoteException;
    
    PaymentRecord getPaymentByItemId(int itemId) throws RemoteException;
    
    List<PaymentRecord> getPaymentsByWinner(String winnerName) throws RemoteException;
    
    List<PaymentRecord> getAllPayments() throws RemoteException;
    
    boolean isPaymentCompleted(int itemId) throws RemoteException;
    
    void confirmPaymentReceived(int paymentId, String adminName) throws RemoteException;
    
    double[] getPaymentStatistics() throws RemoteException;
    
    List<String[]> getPaymentHistory() throws RemoteException;
    
    List<String[]> getPaymentHistoryByItem(int itemId) throws RemoteException;
    
    List<String[]> getPaymentHistoryByWinner(String winnerName) throws RemoteException;
    
    // ============ NEW: Client Complete Payment ============
    
    void completePaymentByClient(int paymentId, String winnerName, String paymentMethod, String transactionId) throws RemoteException;
}