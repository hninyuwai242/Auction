package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    
    // ============ AUCTION NOTIFICATIONS ============
    
    void notifyBidUpdate(int itemId, double newPrice, String currentWinner) throws RemoteException;
    
    void notifyAuctionEnd(int itemId, String winner, double finalPrice) throws RemoteException;
    
    void notifyNewItem(AuctionItem item) throws RemoteException;
    
    void notifyNewAuctionStart(AuctionItem item) throws RemoteException;
    
    void notifyItemListRefresh() throws RemoteException;
    
    // ============ CHAT ============
    
    void receiveChatMessage(String sender, String message) throws RemoteException;
    
    String getClientName() throws RemoteException;
    
    // ============ SCHEDULE ============
    
    void receiveScheduleAlert(String itemName, String startTime, String countdown, String price, long remainingSeconds) throws RemoteException;
    
    // ============ PAYMENT NOTIFICATION ============
    
    void notifyPaymentUpdate(PaymentRecord payment) throws RemoteException;
}