package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class AuctionServer {
    public static final int RMI_PORT = 1099;
    
    public static void main(String[] args) {
        try {
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║  🏛  PREMIUM AUCTION HOUSE - SERVER STARTED              ║");
            System.out.println("║                                                            ║");
            
            System.out.println("║  📡 Starting RMI Registry on port " + RMI_PORT + "...");
            LocateRegistry.createRegistry(RMI_PORT);
            System.out.println("║  ✅ RMI Registry started on port " + RMI_PORT);
            
            System.out.println("║  🔧 Creating Auction Service...");
            AuctionServiceImpl auctionService = new AuctionServiceImpl();
            
            String url = "rmi://localhost:" + RMI_PORT + "/AuctionService";
            System.out.println("║  🔗 Binding service to: " + url);
            Naming.rebind(url, auctionService);
            
            String serverIP = auctionService.getServerIP();
            
            System.out.println("║                                                            ║");
            System.out.println("║  ✅ SERVER STARTED SUCCESSFULLY!                          ║");
            System.out.println("║  📡 Server IP: " + serverIP);
            System.out.println("║  🔌 RMI Port: " + RMI_PORT);
            System.out.println("║  🔗 Service URL: rmi://" + serverIP + ":" + RMI_PORT + "/AuctionService");
            System.out.println("║  💾 Database: SQLite (auction.db)                         ║");
            System.out.println("║                                                            ║");
            System.out.println("║  🎯 SEQUENTIAL AUCTION MODE:                               ║");
            System.out.println("║     - One item at a time                                   ║");
            System.out.println("║     - 12 items total                                       ║");
            System.out.println("║     - Each auction: 120 seconds                            ║");
            System.out.println("║                                                            ║");
            System.out.println("║  💡 Waiting for clients to connect...                      ║");
            System.out.println("║  📝 Clients should connect to: " + serverIP + ":" + RMI_PORT);
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("❌ Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}