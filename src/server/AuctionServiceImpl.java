package server;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionServiceImpl extends UnicastRemoteObject implements AuctionService {
    
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_RMI_PORT = 1099;
    
    private List<AuctionItem> items;
    private List<ClientCallback> clients;
    private Map<Integer, Timer> timers;
    private int nextItemId = 1;
    private String serverIP;
    private int currentActiveIndex = 0;
    private AuctionItem currentActiveItem;
    private ScheduledExecutorService scheduler;
    
    private static final long AUCTION_DURATION_SECONDS = 120;
    private static final long BREAK_TIME_SECONDS = 5;
    
    private List<AuctionSchedule> schedules;
    private int nextScheduleId = 1;
    private boolean scheduledModeEnabled = false;
    
    private AuctionSchedule nextPendingSchedule = null;
    private int lastAlertSentScheduleId = -1;
    
    // Payment related
    private List<PaymentRecord> paymentRecords;
    
    // ============ CONSTRUCTOR ============
    
    public AuctionServiceImpl() throws RemoteException {
        super();
        items = new CopyOnWriteArrayList<>();
        clients = new CopyOnWriteArrayList<>();
        timers = new ConcurrentHashMap<>();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        schedules = new CopyOnWriteArrayList<>();
        paymentRecords = new CopyOnWriteArrayList<>();
        
        try {
            serverIP = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            serverIP = "127.0.0.1";
        }
        
        loadItemsFromDatabase();
        
        if (items.isEmpty()) {
            addSampleItems();
        }
        
        loadSchedulesFromDatabase();
        loadPaymentsFromDatabase();
        findNextPendingSchedule();
        startScheduleChecker();
        
        // ============================================================
        // ============ FIX: NEVER auto-start auction ============
        // ============================================================
        // Deactivate any items that might be stuck in "active" state
        deactivateAllItems();
        
        // Clear any active auction state
        currentActiveItem = null;
        currentActiveIndex = 0;
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  🏛  AUCTION SERVER STARTED - WAITING MODE               ║");
        System.out.println("║                                                            ║");
        System.out.println("║  📌 NO AUCTION WILL START AUTOMATICALLY!                   ║");
        System.out.println("║  📅 Scheduled Mode: " + (scheduledModeEnabled ? "ENABLED" : "DISABLED"));
        System.out.println("║  📅 Schedules count: " + schedules.size());
        System.out.println("║                                                            ║");
        System.out.println("║  ⏰ To start an auction:                                    ║");
        System.out.println("║     1. Create a schedule (Server GUI)                      ║");
        System.out.println("║     2. Enable Scheduled Mode (Server GUI)                  ║");
        System.out.println("║     3. Wait for scheduled time to arrive                   ║");
        System.out.println("║                                                            ║");
        System.out.println("║  💡 Or use 'Start Now' button in Server GUI               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Broadcast initial status to any connected clients
        broadcastScheduleInfo();
    }
    
    // ============ DEACTIVATE ALL ITEMS ============
    
    private void deactivateAllItems() {
        try {
            for (AuctionItem item : items) {
                if (item.isActive()) {
                    item.setActive(false);
                    DatabaseHelper.updateItemActiveStatus(item.getId(), false);
                    System.out.println("🔒 Deactivated item: " + item.getName() + " (ID: " + item.getId() + ")");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error deactivating items: " + e.getMessage());
        }
    }
    
    // ============ LOAD PAYMENTS FROM DATABASE ============
    
    private void loadPaymentsFromDatabase() {
        try {
            List<PaymentRecord> dbPayments = DatabaseHelper.getAllPaymentRecords();
            if (!dbPayments.isEmpty()) {
                paymentRecords.addAll(dbPayments);
                System.out.println("💳 Loaded " + paymentRecords.size() + " payment records from database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading payments from database: " + e.getMessage());
        }
    }
    
    // ============ UTILITY METHOD ============
    
    private boolean canStartAuction() {
        // Only start if scheduled mode is enabled
        if (!scheduledModeEnabled) {
            System.out.println("🚫 Cannot start auction - Scheduled mode is DISABLED");
            return false;
        }
        
        // Check if there's a pending schedule
        for (AuctionSchedule s : schedules) {
            if ("pending".equals(s.getStatus())) {
                return true;
            }
        }
        
        System.out.println("🚫 Cannot start auction - No pending schedules found");
        return false;
    }
    
    // ============ LOAD FROM DATABASE METHODS ============
    
    private void loadItemsFromDatabase() {
        try {
            List<AuctionItem> dbItems = DatabaseHelper.getAllItems();
            if (!dbItems.isEmpty()) {
                items.addAll(dbItems);
                for (AuctionItem item : items) {
                    if (item.getId() >= nextItemId) {
                        nextItemId = item.getId() + 1;
                    }
                }
                System.out.println("📦 Loaded " + items.size() + " items from database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading items from database: " + e.getMessage());
        }
    }
    
    private void loadSchedulesFromDatabase() {
        try {
            List<AuctionSchedule> dbSchedules = DatabaseHelper.getAllSchedules();
            if (!dbSchedules.isEmpty()) {
                schedules.addAll(dbSchedules);
                for (AuctionSchedule s : schedules) {
                    if (s.getId() >= nextScheduleId) {
                        nextScheduleId = s.getId() + 1;
                    }
                }
                System.out.println("📅 Loaded " + schedules.size() + " schedules from database");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error loading schedules from database: " + e.getMessage());
        }
    }
    
    // ============ SAMPLE ITEMS ============
    
    private byte[] loadImageFromFile(String fileName) {
        try {
            String[] paths = {
                fileName,
                "images/" + fileName,
                "server/images/" + fileName,
                "../images/" + fileName,
                System.getProperty("user.dir") + "/images/" + fileName
            };
            
            for (String path : paths) {
                File file = new File(path);
                if (file.exists()) {
                    System.out.println("   📷 Loading image from: " + file.getAbsolutePath());
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "jpg", baos);
                        System.out.println("   ✅ Image loaded: " + fileName + " (" + baos.size() + " bytes)");
                        return baos.toByteArray();
                    }
                }
            }
            
            java.net.URL url = getClass().getResource("/images/" + fileName);
            if (url != null) {
                System.out.println("   📷 Loading image from resources: " + fileName);
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "jpg", baos);
                    System.out.println("   ✅ Image loaded from resources: " + fileName);
                    return baos.toByteArray();
                }
            }
            
            System.out.println("   ⚠️ Image file not found: " + fileName);
            return null;
            
        } catch (Exception e) {
            System.out.println("   ⚠️ Error loading image " + fileName + ": " + e.getMessage());
            return null;
        }
    }
    
    private byte[] createBeautifulItemImage(String icon, String itemName, Color bgColor1, Color bgColor2) {
        try {
            int width = 100, height = 100;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            GradientPaint gradient = new GradientPaint(0, 0, bgColor1, width, height, bgColor2);
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
            
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(2, 2, width-5, height-5, 12, 12);
            
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(3, 3, width-7, height-7, 10, 10);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 55));
            FontMetrics fm = g2d.getFontMetrics();
            String displayIcon = icon != null && !icon.isEmpty() ? icon : getDefaultIcon(itemName);
            int x = (width - fm.stringWidth(displayIcon)) / 2;
            int y = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(displayIcon, x, y);
            
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval(width-15, height-15, 8, 8);
            g2d.fillOval(7, 7, 5, 5);
            
            g2d.dispose();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            System.err.println("Error creating image: " + e.getMessage());
            return null;
        }
    }
    
    private String getDefaultIcon(String itemName) {
        if (itemName.contains("Watch") || itemName.contains("Rolex") || itemName.contains("Omega")) return "⌚";
        if (itemName.contains("Laptop") || itemName.contains("MacBook")) return "💻";
        if (itemName.contains("Painting")) return "🎨";
        if (itemName.contains("Guitar") || itemName.contains("Fender")) return "🎸";
        if (itemName.contains("Phone") || itemName.contains("iPhone")) return "📱";
        if (itemName.contains("Camera") || itemName.contains("Canon")) return "📷";
        if (itemName.contains("Shoes") || itemName.contains("Nike")) return "👟";
        if (itemName.contains("Bag") || itemName.contains("Louis")) return "👜";
        if (itemName.contains("Headphones") || itemName.contains("Sony")) return "🎧";
        if (itemName.contains("Drone") || itemName.contains("DJI")) return "🚁";
        if (itemName.contains("Bicycle") || itemName.contains("Trek")) return "🚲";
        return "🖼";
    }
    
    private Color getColorForItem(String itemName, boolean isLight) {
        int hash = itemName.hashCode();
        float hue = (Math.abs(hash) % 360) / 360f;
        if (isLight) {
            return Color.getHSBColor(hue, 0.7f, 0.9f);
        } else {
            return Color.getHSBColor(hue, 0.8f, 0.6f);
        }
    }
    
    private void addSampleItems() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║           LOADING 20 AUCTION ITEMS                        ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        Object[][] sampleData = {
            // Original 12 items
            {"Vintage Rolex Watch", "1950s Rare Rolex, Automatic movement, Excellent condition", 500.00, "⌚", "rolex.jpg"},
            {"MacBook Pro M3", "16-inch, M3 Max chip, 64GB RAM, 2TB SSD, Space Black", 1200.00, "💻", "macbook.jpg"},
            {"Original Oil Painting", "Hand-painted by renowned artist, Abstract Expressionism", 800.00, "🎨", "painting.jpg"},
            {"Fender Stratocaster", "1965 Vintage, Mint Condition, Original Parts, Case included", 2500.00, "🎸", "guitar.jpg"},
            {"iPhone 15 Pro Max", "512GB, Titanium, Unlocked, 1 year warranty", 900.00, "📱", "iphone.jpg"},
            {"Canon EOS R5 Camera", "45MP Full-frame, 8K video, RF lens included", 2800.00, "📷", "camera.jpg"},
            {"Nike Air Jordan 1", "Limited Edition, Size 10, Never worn, Original box", 300.00, "👟", "shoes.jpg"},
            {"Louis Vuitton Bag", "Authentic, Limited edition, Excellent condition", 1500.00, "👜", "bag.jpg"},
            {"Sony WH-1000XM5", "Noise Cancelling Headphones, Like new, With case", 250.00, "🎧", "headphones.jpg"},
            {"DJI Mavic 3 Drone", "Pro Cine Premium Combo, 3 batteries, Hard case", 1800.00, "🚁", "drone.jpg"},
            {"Trek Mountain Bike", "Carbon frame, Full suspension, 12-speed", 2200.00, "🚲", "bike.jpg"},
            {"Omega Seamaster Watch", "Diver 300M, Automatic, Ceramic bezel", 3200.00, "⌚", "omega.jpg"},
            
            // New 8 items
            {"Samsung Galaxy S24 Ultra", "512GB, Titanium, 200MP Camera, S Pen included", 1100.00, "📱", "samsung.jpg"},
            {"Dell XPS 16 Laptop", "16-inch OLED, Intel Ultra 9, 64GB RAM, 4TB SSD", 1800.00, "💻", "dell.jpg"},
            {"Gibson Les Paul Guitar", "1959 Reissue, Sunburst, Hard case included", 3500.00, "🎸", "gibson.jpg"},
            {"Panasonic Lumix GH6", "25MP Micro Four Thirds, 5.7K video, Weather sealed", 2000.00, "📷", "lumix.jpg"},
            {"Apple Watch Ultra 2", "49mm Titanium, GPS + Cellular, 1 year warranty", 700.00, "⌚", "applewatch.jpg"},
            {"Bose QC45 Headphones", "Noise Cancelling, 24hr battery, Bluetooth 5.1", 300.00, "🎧", "bose.jpg"},
            {"Prada Leather Bag", "Saffiano Leather, Gold hardware, Authentic", 1800.00, "👜", "prada.jpg"},
            {"Specialized Mountain Bike", "Carbon fiber frame, Full suspension, 12-speed", 2800.00, "🚲", "specialized.jpg"}
        };
        
        for (int i = 0; i < sampleData.length; i++) {
            String name = (String) sampleData[i][0];
            String desc = (String) sampleData[i][1];
            double price = (Double) sampleData[i][2];
            String icon = (String) sampleData[i][3];
            String imageFileName = (String) sampleData[i][4];
            
            byte[] imageData = loadImageFromFile(imageFileName);
            
            if (imageData == null) {
                System.out.println("   ⚠️ Image file '" + imageFileName + "' not found, creating fallback icon image");
                Color lightColor = getColorForItem(name, true);
                Color darkColor = getColorForItem(name, false);
                imageData = createBeautifulItemImage(icon, name, lightColor, darkColor);
            }
            
            AuctionItem item = new AuctionItem(nextItemId, name, desc, price, 
                                               AUCTION_DURATION_SECONDS, imageData, imageFileName);
            
            try {
                int id = DatabaseHelper.insertItem(item);
                if (id > 0) {
                    item.setId(id);
                    items.add(item);
                    nextItemId = id + 1;
                    System.out.println((i+1) + ". " + name + " (ID: " + id + ")");
                    System.out.println("   💰 Starting Price: $" + price);
                    System.out.println("   🖼️ Image: " + (imageData != null ? imageData.length + " bytes" : "null"));
                    System.out.println();
                }
            } catch (SQLException e) {
                System.err.println("❌ Error saving item to database: " + e.getMessage());
            }
        }
        
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     TOTAL " + items.size() + " ITEMS SAVED TO DATABASE               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }
    
    // ============ ADD ITEM METHODS ============
    
    @Override
    public void addNewItemWithImage(String name, String description, double startingPrice, byte[] imageData, String imageFileName) throws RemoteException {
        try {
            byte[] finalImageData = imageData;
            
            if (finalImageData == null || finalImageData.length == 0) {
                String icon = getDefaultIcon(name);
                Color lightColor = getColorForItem(name, true);
                Color darkColor = getColorForItem(name, false);
                finalImageData = createBeautifulItemImage(icon, name, lightColor, darkColor);
                imageFileName = name.toLowerCase().replace(" ", "_") + ".png";
                System.out.println("⚠️ No image provided, created fallback icon image for: " + name);
            }
            
            AuctionItem newItem = new AuctionItem(nextItemId, name, description, startingPrice, 
                                                  AUCTION_DURATION_SECONDS, finalImageData, imageFileName);
            
            int id = DatabaseHelper.insertItem(newItem);
            if (id > 0) {
                newItem.setId(id);
                items.add(newItem);
                nextItemId = id + 1;
                
                for (ClientCallback client : clients) {
                    try {
                        client.notifyNewItem(newItem);
                        client.receiveChatMessage("🆕 NEW ITEM", "New item added: " + name + " - $" + startingPrice);
                    } catch (RemoteException e) {
                        clients.remove(client);
                    }
                }
                
                System.out.println("✅ New item added to database: " + name + " (ID: " + id + ")");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error adding item to database: " + e.getMessage());
            throw new RemoteException("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error adding item: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void addNewItem(String name, String description, double startingPrice, byte[] imageData) throws RemoteException {
        addNewItemWithImage(name, description, startingPrice, imageData, name.toLowerCase().replace(" ", "_") + ".jpg");
    }
    
    @Override
    public void addItem(AuctionItem item) throws RemoteException {
        try {
            int id = DatabaseHelper.insertItem(item);
            if (id > 0) {
                item.setId(id);
                items.add(item);
                nextItemId = id + 1;
                
                for (ClientCallback client : clients) {
                    try {
                        client.receiveChatMessage("🆕 NEW ITEM", item.getName() + " added!");
                    } catch (RemoteException e) {
                        clients.remove(client);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    // ============ AUCTION CONTROL METHODS ============
    
    public void startAuctionNow(int itemId) throws RemoteException {
        AuctionItem item = findItemById(itemId);
        if (item == null) {
            throw new RemoteException("Item not found with ID: " + itemId);
        }
        
        if (item.isSold()) {
            throw new RemoteException("Item is already sold!");
        }
        
        if (currentActiveItem != null && currentActiveItem.isActive()) {
            throw new RemoteException("An auction is already running: " + currentActiveItem.getName());
        }
        
        // Start the auction immediately
        item.setActive(true);
        item.setSold(false);
        item.setCurrentPrice(item.getStartingPrice());
        item.setCurrentWinner("");
        item.setEndTime(System.currentTimeMillis() + (AUCTION_DURATION_SECONDS * 1000));
        item.setWinnerName("");
        
        currentActiveItem = item;
        
        try {
            DatabaseHelper.updateItem(item);
            DatabaseHelper.updateItemActiveStatus(item.getId(), true);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
        
        System.out.println("\n🎯 MANUAL AUCTION START: " + item.getName());
        System.out.println("   💰 Starting bid: $" + item.getStartingPrice());
        System.out.println("   ⏱️ Duration: " + AUCTION_DURATION_SECONDS + " seconds");
        
        startAuctionTimer(item);
        broadcastAuctionStart(item);
        broadcastChatMessage("🎯 MANUAL START", "🔥 " + item.getName() + " started manually!");
    }
    
    private void startNextAuction() {
        if (!scheduledModeEnabled) {
            System.out.println("📅 NOT starting auction - Scheduled mode is DISABLED");
            return;
        }
        
        for (AuctionItem item : items) {
            if (!item.isSold()) {
                for (AuctionSchedule s : schedules) {
                    if (s.getItemId() == item.getId() && "pending".equals(s.getStatus())) {
                        if (s.getScheduledStartTime() != null) {
                            long scheduledTime = s.getScheduledStartTime().getTime();
                            long currentTime = System.currentTimeMillis();
                            if (scheduledTime <= currentTime) {
                                startAuctionFromSchedule(s);
                                return;
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        System.out.println("📅 No pending schedules ready to start");
    }
    
    private void startAuctionTimer(AuctionItem item) {
        long remainingSeconds = item.getRemainingSeconds();
        System.out.println("⏰ Starting timer for " + item.getName() + " with " + remainingSeconds + " seconds remaining");
        
        if (remainingSeconds <= 0) {
            System.out.println("⚠️ Timer already expired, ending auction immediately");
            try {
                endAuction(item);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return;
        }
        
        scheduler.schedule(() -> {
            try {
                System.out.println("⏰ TIMER TRIGGERED for " + item.getName());
                endAuction(item);
            } catch (RemoteException e) {
                System.err.println("Error ending auction: " + e.getMessage());
                e.printStackTrace();
            }
        }, remainingSeconds, TimeUnit.SECONDS);
        
        System.out.println("⏰ Timer scheduled for " + item.getName() + " in " + remainingSeconds + " seconds");
    }
    
    private void endAuction(AuctionItem item) throws RemoteException {
        System.out.println("🏁 endAuction() called for: " + item.getName());
        System.out.println("   Active: " + item.isActive() + ", Sold: " + item.isSold());
        
        if (!item.isActive() || item.isSold()) {
            System.out.println("   Auction already ended, skipping...");
            return;
        }
        
        item.setActive(false);
        item.setSold(true);
        
        String winner = item.getCurrentWinner().isEmpty() ? "No bids" : item.getCurrentWinner();
        double finalPrice = item.getCurrentPrice();
        item.setWinnerName(winner);
        
        try {
            DatabaseHelper.updateItem(item);
            DatabaseHelper.updateItemActiveStatus(item.getId(), false);
            DatabaseHelper.insertAuctionHistory(item.getId(), item.getName(), winner, finalPrice);
            DatabaseHelper.updateScheduleStatusByItemId(item.getId(), "ended");
            
            // Create Payment Record
            if (!winner.equals("No bids")) {
                createPaymentRecord(item.getId(), winner, finalPrice);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error saving auction result to database: " + e.getMessage());
        }
        
        System.out.println("\n🏁 AUCTION ENDED: " + item.getName());
        System.out.println("   Winner: " + winner);
        System.out.println("   Price: $" + finalPrice);
        
        for (ClientCallback client : clients) {
            try {
                client.notifyAuctionEnd(item.getId(), winner, finalPrice);
                client.receiveChatMessage("⏰ INFO", "Next auction in 5 seconds...");
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
        
        broadcastChatMessage("🏆 RESULT", winner.equals("No bids") ? 
            "😢 " + item.getName() + " - No bids!" : 
            "🏆 " + winner.toUpperCase() + " won " + item.getName() + " for $" + String.format("%.2f", finalPrice));
        
        if (!winner.equals("No bids")) {
            broadcastChatMessage("💳 PAYMENT", 
                "💰 " + winner + " please complete payment of $" + String.format("%.2f", finalPrice) + 
                " for " + item.getName());
        }
        
        timers.remove(item.getId());
        currentActiveItem = null;
        
        findNextPendingSchedule();
        
        scheduler.schedule(() -> {
            System.out.println("🔄 Checking for next scheduled auction...");
            checkAndStartScheduledAuctions();
            for (ClientCallback client : clients) {
                try {
                    client.receiveChatMessage("🔄 SYSTEM", "Checking for next auction...");
                    client.notifyItemListRefresh();
                } catch (RemoteException e) {
                    clients.remove(client);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }
    
    // ============ SCHEDULE METHODS ============
    
    private void startScheduleChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            if (scheduledModeEnabled) {
                checkAndStartScheduledAuctions();
                broadcastCountdownNotifications();
                checkAndSendNewScheduleAlert();
            }
            
            findNextPendingSchedule();
            
        }, 0, 1, TimeUnit.SECONDS);
        
        System.out.println("⏰ Schedule checker started - checks every 1 second");
        System.out.println("   📌 Auctions will ONLY start when Scheduled Mode is ENABLED");
    }
    
    private void checkAndStartScheduledAuctions() {
        if (!scheduledModeEnabled) {
            return;
        }
        
        if (currentActiveItem != null && currentActiveItem.isActive() && !currentActiveItem.isSold()) {
            return;
        }
        
        Date now = new Date();
        
        for (AuctionSchedule schedule : schedules) {
            if ("pending".equals(schedule.getStatus())) {
                if (schedule.getScheduledStartTime() != null) {
                    long scheduledTime = schedule.getScheduledStartTime().getTime();
                    long currentTime = now.getTime();
                    long diff = scheduledTime - currentTime;
                    
                    if (diff <= 0) {
                        System.out.println("   ✅ Starting scheduled auction: " + schedule.getItemName());
                        startAuctionFromSchedule(schedule);
                        return;
                    }
                }
            }
        }
    }
    
    private void startAuctionFromSchedule(AuctionSchedule schedule) {
        System.out.println("🎯 Starting scheduled auction for: " + schedule.getItemName());
        
        AuctionItem item = findItemById(schedule.getItemId());
        if (item == null) {
            System.out.println("❌ Item not found for schedule: " + schedule.getItemId());
            schedule.setStatus("cancelled");
            try {
                DatabaseHelper.updateScheduleStatus(schedule.getId(), "cancelled");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (item.isSold()) {
            System.out.println("⚠️ Item already sold: " + item.getName());
            schedule.setStatus("cancelled");
            try {
                DatabaseHelper.updateScheduleStatus(schedule.getId(), "cancelled");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (item.isActive()) {
            System.out.println("⚠️ Item is already active: " + item.getName());
            schedule.setStatus("active");
            try {
                DatabaseHelper.updateScheduleStatus(schedule.getId(), "active");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return;
        }
        
        schedule.setStatus("active");
        try {
            DatabaseHelper.updateScheduleStatus(schedule.getId(), "active");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        item.setActive(true);
        item.setSold(false);
        item.setCurrentPrice(item.getStartingPrice());
        item.setCurrentWinner("");
        item.setEndTime(System.currentTimeMillis() + (AUCTION_DURATION_SECONDS * 1000));
        item.setWinnerName("");
        
        try {
            DatabaseHelper.updateItem(item);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        currentActiveItem = item;
        
        findNextPendingSchedule();
        
        System.out.println("\n🎯 STARTING SCHEDULED AUCTION: " + item.getName());
        System.out.println("   💰 Starting bid: $" + item.getStartingPrice());
        System.out.println("   ⏱️ Duration: " + AUCTION_DURATION_SECONDS + " seconds");
        System.out.println("   ⏰ Started at: " + new Date());
        
        startAuctionTimer(item);
        broadcastAuctionStart(item);
        broadcastScheduleInfo();
        
        for (ClientCallback client : clients) {
            try {
                client.receiveChatMessage("🎯 AUCTION START", 
                    "🔥 " + item.getName() + " is NOW OPEN for bidding!");
                client.notifyNewAuctionStart(item);
                client.notifyItemListRefresh();
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
    }
    
    // ============ SCHEDULE NOTIFICATION METHODS ============
    
    private void findNextPendingSchedule() {
        nextPendingSchedule = null;
        Date now = new Date();
        
        for (AuctionSchedule schedule : schedules) {
            if ("pending".equals(schedule.getStatus())) {
                if (schedule.getScheduledStartTime() != null && 
                    schedule.getScheduledStartTime().getTime() > now.getTime()) {
                    if (nextPendingSchedule == null || 
                        schedule.getScheduledStartTime().getTime() < nextPendingSchedule.getScheduledStartTime().getTime()) {
                        nextPendingSchedule = schedule;
                    }
                }
            }
        }
        
        if (nextPendingSchedule != null && nextPendingSchedule.getId() != lastAlertSentScheduleId) {
            lastAlertSentScheduleId = -1;
        }
    }
    
    @Override
    public List<String[]> getUpcomingSchedulesInfo() throws RemoteException {
        List<String[]> info = new ArrayList<>();
        Date now = new Date();
        
        for (AuctionSchedule schedule : schedules) {
            if ("pending".equals(schedule.getStatus())) {
                if (schedule.getScheduledStartTime() != null && 
                    schedule.getScheduledStartTime().getTime() > now.getTime()) {
                    
                    long remainingMillis = schedule.getScheduledStartTime().getTime() - now.getTime();
                    long remainingSeconds = remainingMillis / 1000;
                    
                    String timeStr = formatCountdown(remainingSeconds);
                    
                    info.add(new String[]{
                        schedule.getItemName(),
                        schedule.getFormattedRemainingTime(),
                        timeStr,
                        String.format("%.2f", schedule.getStartingPrice()),
                        formatDateTime(schedule.getScheduledStartTime())
                    });
                }
            }
        }
        return info;
    }
    
    @Override
    public Date getNextAuctionTime() throws RemoteException {
        findNextPendingSchedule();
        if (nextPendingSchedule != null) {
            return nextPendingSchedule.getScheduledStartTime();
        }
        return null;
    }
    
    @Override
    public long getNextAuctionCountdown() throws RemoteException {
        findNextPendingSchedule();
        if (nextPendingSchedule != null) {
            long remaining = nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : 0;
        }
        return -1;
    }
    
    @Override
    public String[] getNextAuctionDetails() throws RemoteException {
        findNextPendingSchedule();
        if (nextPendingSchedule != null) {
            long remainingSeconds = (nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis()) / 1000;
            
            return new String[]{
                nextPendingSchedule.getItemName(),
                formatDateTime(nextPendingSchedule.getScheduledStartTime()),
                formatCountdown(remainingSeconds),
                String.format("%.2f", nextPendingSchedule.getStartingPrice()),
                String.valueOf(remainingSeconds)
            };
        }
        return null;
    }
    
    private String formatCountdown(long seconds) {
        if (seconds <= 0) return "Starting now!";
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + secs + "s";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        } else if (minutes > 0) {
            return minutes + "m " + secs + "s";
        } else {
            return secs + "s";
        }
    }
    
    private String formatDateTime(Date date) {
        if (date == null) return "N/A";
        LocalDateTime localDateTime = date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        return localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
    }
    
    private void broadcastScheduleInfo() {
        findNextPendingSchedule();
        
        if (nextPendingSchedule != null) {
            long remainingSeconds = (nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis()) / 1000;
            if (remainingSeconds > 0) {
                String timeStr = formatCountdown(remainingSeconds);
                try {
                    broadcastChatMessage("📅 NEXT AUCTION", 
                        "⏰ Next auction: " + nextPendingSchedule.getItemName() + 
                        " starts in " + timeStr + 
                        " ($" + String.format("%.2f", nextPendingSchedule.getStartingPrice()) + ")");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                broadcastChatMessage("📅 SCHEDULE", "No upcoming auctions scheduled.");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendScheduleAlertToClient(ClientCallback callback) {
        try {
            findNextPendingSchedule();
            
            if (nextPendingSchedule != null) {
                long remainingSeconds = (nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis()) / 1000;
                
                String itemName = nextPendingSchedule.getItemName();
                String startTime = formatDateTime(nextPendingSchedule.getScheduledStartTime());
                String price = String.format("%.2f", nextPendingSchedule.getStartingPrice());
                String countdown = formatCountdown(remainingSeconds);
                
                callback.receiveScheduleAlert(itemName, startTime, countdown, price, remainingSeconds);
                callback.receiveChatMessage("📅 NEXT AUCTION", 
                    "⏰ Next auction starts in " + countdown + "\n" +
                    "📦 Item: " + itemName + "\n" +
                    "💰 Starting Price: $" + price);
                
                lastAlertSentScheduleId = nextPendingSchedule.getId();
                
                System.out.println("📨 Schedule alert sent to client: " + itemName + " at " + startTime);
                
            } else {
                callback.receiveChatMessage("📅 SCHEDULE", "No upcoming auctions scheduled.");
            }
        } catch (RemoteException e) {
            // ignore
        }
    }
    
    private void checkAndSendNewScheduleAlert() {
        if (!scheduledModeEnabled) return;
        
        findNextPendingSchedule();
        if (nextPendingSchedule != null) {
            if (nextPendingSchedule.getId() != lastAlertSentScheduleId) {
                long remainingSeconds = (nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis()) / 1000;
                
                if (remainingSeconds <= 3600 && remainingSeconds > 0) {
                    broadcastScheduleAlertToAll();
                    lastAlertSentScheduleId = nextPendingSchedule.getId();
                }
            }
        }
    }
    
    private void broadcastScheduleAlertToAll() {
        try {
            String[] details = getNextAuctionDetails();
            if (details != null) {
                String itemName = details[0];
                String startTime = details[1];
                String countdown = details[2];
                String price = details[3];
                long remainingSeconds = Long.parseLong(details[4]);
                
                for (ClientCallback client : clients) {
                    try {
                        client.receiveScheduleAlert(itemName, startTime, countdown, price, remainingSeconds);
                        client.receiveChatMessage("📅 ALERT", 
                            "🔔 NEW AUCTION SCHEDULED!\n" +
                            "📦 " + itemName + "\n" +
                            "⏰ Starts at: " + startTime + "\n" +
                            "⌛ " + countdown + " remaining\n" +
                            "💰 Starting Price: $" + price);
                    } catch (RemoteException e) {
                        clients.remove(client);
                    }
                }
                
                System.out.println("📨 Schedule alert broadcast to all clients: " + itemName);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    private void broadcastCountdownNotifications() {
        if (!scheduledModeEnabled) return;
        
        findNextPendingSchedule();
        if (nextPendingSchedule != null) {
            long remainingMillis = nextPendingSchedule.getRemainingMillis();
            if (remainingMillis <= 0) return;
            
            long remainingSeconds = remainingMillis / 1000;
            
            long[] intervals = {3600, 1800, 600, 300, 120, 60, 30, 10, 5, 3, 2, 1};
            
            for (long interval : intervals) {
                if (remainingSeconds == interval) {
                    try {
                        String timeStr = formatCountdown(remainingSeconds);
                        String itemName = nextPendingSchedule.getItemName();
                        
                        broadcastChatMessage("⏰ COUNTDOWN", 
                            "🔔 " + itemName + " starts in " + timeStr + "!");
                        
                        if (remainingSeconds <= 60) {
                            String startTime = formatDateTime(nextPendingSchedule.getScheduledStartTime());
                            String price = String.format("%.2f", nextPendingSchedule.getStartingPrice());
                            
                            for (ClientCallback client : clients) {
                                try {
                                    client.receiveScheduleAlert(
                                        itemName, 
                                        startTime, 
                                        timeStr, 
                                        price, 
                                        remainingSeconds
                                    );
                                } catch (RemoteException e) {
                                    clients.remove(client);
                                }
                            }
                            
                            System.out.println("⏰ Countdown alert sent: " + itemName + " starts in " + timeStr);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
    
    // ============ SCHEDULE MANAGEMENT METHODS ============
    
    @Override
    public void enableScheduledMode(boolean enable) throws RemoteException {
        this.scheduledModeEnabled = enable;
        System.out.println("📅 Scheduled mode: " + (enable ? "ENABLED" : "DISABLED"));
        
        if (enable) {
            broadcastChatMessage("📅 SYSTEM", "✅ Scheduled auction mode ENABLED");
            broadcastChatMessage("📅 SYSTEM", "⏰ Auctions will start at their scheduled times");
            
            findNextPendingSchedule();
            broadcastScheduleInfo();
            broadcastScheduleAlertToAll();
            
            checkAndStartScheduledAuctions();
            
        } else {
            broadcastChatMessage("📅 SYSTEM", "⏸ Scheduled auction mode DISABLED");
            broadcastChatMessage("📅 SYSTEM", "📌 No auctions will start until mode is ENABLED");
        }
    }
    
    @Override
    public void createAuctionSchedule(int itemId, Date startTime) throws RemoteException {
        AuctionItem item = findItemById(itemId);
        if (item == null) {
            throw new RemoteException("Item not found with ID: " + itemId);
        }
        
        if (startTime.getTime() <= System.currentTimeMillis()) {
            throw new RemoteException("Start time must be in the future!");
        }
        
        for (AuctionSchedule s : schedules) {
            if (s.getItemId() == itemId && "pending".equals(s.getStatus())) {
                throw new RemoteException("Item already has a pending schedule!");
            }
        }
        
        Date endTime = new Date(startTime.getTime() + (AUCTION_DURATION_SECONDS * 1000));
        
        AuctionSchedule schedule = new AuctionSchedule(
            nextScheduleId,
            itemId,
            item.getName(),
            item.getStartingPrice(),
            startTime,
            endTime,
            "pending"
        );
        
        try {
            int id = DatabaseHelper.insertSchedule(schedule);
            if (id > 0) {
                schedule.setId(id);
                schedules.add(schedule);
                nextScheduleId = id + 1;
                
                findNextPendingSchedule();
                
                for (ClientCallback client : clients) {
                    try {
                        client.receiveChatMessage("📅 SCHEDULE", 
                            "📅 New auction scheduled: " + item.getName() + 
                            " at " + formatDateTime(startTime));
                        client.notifyItemListRefresh();
                    } catch (RemoteException e) {
                        clients.remove(client);
                    }
                }
                
                System.out.println("📅 Schedule created: " + item.getName() + " at " + startTime);
                
                if (!scheduledModeEnabled) {
                    broadcastChatMessage("📅 SYSTEM", "⚠️ Scheduled mode is DISABLED. Enable it to activate this schedule.");
                }
            }
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public List<AuctionSchedule> getPendingSchedules() throws RemoteException {
        List<AuctionSchedule> pending = new ArrayList<>();
        for (AuctionSchedule s : schedules) {
            if ("pending".equals(s.getStatus())) {
                pending.add(s);
            }
        }
        return pending;
    }
    
    @Override
    public void cancelSchedule(int scheduleId) throws RemoteException {
        for (AuctionSchedule s : schedules) {
            if (s.getId() == scheduleId && "pending".equals(s.getStatus())) {
                s.setStatus("cancelled");
                try {
                    DatabaseHelper.updateScheduleStatus(scheduleId, "cancelled");
                } catch (SQLException e) {
                    throw new RemoteException("Database error: " + e.getMessage());
                }
                broadcastChatMessage("📅 CANCELLED", 
                    "❌ Auction cancelled: " + s.getItemName());
                System.out.println("📅 Schedule cancelled: " + s.getItemName());
                
                findNextPendingSchedule();
                return;
            }
        }
        throw new RemoteException("Schedule not found or already active");
    }
    
    // ============ BID AND CLIENT METHODS ============
    
    @Override
    public boolean placeBid(int itemId, double amount, String bidderName, ClientCallback callback) throws RemoteException {
        AuctionItem item = findItemById(itemId);
        
        if (item == null) return false;
        
        if (currentActiveItem == null || currentActiveItem.getId() != itemId) {
            if (currentActiveItem != null) {
                callback.receiveChatMessage("⚠️ SYSTEM", "Only \"" + currentActiveItem.getName() + "\" is open for bidding!");
            } else {
                callback.receiveChatMessage("⚠️ SYSTEM", "No auction is currently active!");
            }
            return false;
        }
        
        if (!item.isActive() || item.isSold()) {
            callback.receiveChatMessage("⚠️ SYSTEM", "This auction has ended!");
            return false;
        }
        
        if (amount <= item.getCurrentPrice()) {
            callback.receiveChatMessage("⚠️ SYSTEM", "Bid must be higher than $" + String.format("%.2f", item.getCurrentPrice()));
            return false;
        }
        
        item.setCurrentPrice(amount);
        item.setCurrentWinner(bidderName);
        
        try {
            DatabaseHelper.insertBidHistory(itemId, bidderName, amount);
            DatabaseHelper.updateItem(item);
        } catch (SQLException e) {
            System.err.println("❌ Error saving bid to database: " + e.getMessage());
        }
        
        System.out.println("✅ Bid: " + bidderName + " $" + amount + " on " + item.getName());
        
        broadcastBidUpdate(itemId, amount, bidderName);
        broadcastChatMessage("💰 BID", bidderName + " bid $" + String.format("%.2f", amount) + " on " + item.getName());
        
        return true;
    }
    
    @Override
    public void registerClient(ClientCallback callback) throws RemoteException {
        clients.add(callback);
        
        String clientName = callback.getClientName();
        broadcastChatMessage("💬 SYSTEM", clientName + " joined! (Total: " + clients.size() + ")");
        
        findNextPendingSchedule();
        
        if (nextPendingSchedule != null) {
            long remainingSeconds = (nextPendingSchedule.getScheduledStartTime().getTime() - System.currentTimeMillis()) / 1000;
            String itemName = nextPendingSchedule.getItemName();
            String startTime = formatDateTime(nextPendingSchedule.getScheduledStartTime());
            String price = String.format("%.2f", nextPendingSchedule.getStartingPrice());
            String countdown = formatCountdown(remainingSeconds);
            
            callback.receiveScheduleAlert(itemName, startTime, countdown, price, remainingSeconds);
            callback.receiveChatMessage("📅 NEXT AUCTION", 
                "⏰ Next auction starts in " + countdown + "\n" +
                "📦 Item: " + itemName + "\n" +
                "💰 Starting Price: $" + price);
            
            if (scheduledModeEnabled) {
                callback.receiveChatMessage("📅 STATUS", "✅ Scheduled mode is ENABLED - Auction will start automatically!");
            } else {
                callback.receiveChatMessage("📅 STATUS", "⏸ Scheduled mode is DISABLED - Waiting for admin to enable!");
                callback.receiveChatMessage("📅 STATUS", "📌 Admin must click 'Enable Scheduled Mode' to activate this auction");
            }
            
        } else if (currentActiveItem != null && currentActiveItem.isActive() && !currentActiveItem.isSold()) {
            callback.receiveChatMessage("🎯 CURRENT", "🔥 " + currentActiveItem.getName() + " is currently running!");
            callback.notifyNewAuctionStart(currentActiveItem);
            
        } else {
            callback.receiveChatMessage("📅 SCHEDULE", "📌 No upcoming auctions scheduled.");
            callback.receiveChatMessage("⏰ STATUS", "⏳ Waiting for admin to create schedule and enable scheduled mode.");
            callback.receiveChatMessage("📅 INSTRUCTION", "Admin: Create schedule in Server GUI, then click 'Enable Scheduled Mode'");
        }
        
        for (AuctionSchedule schedule : schedules) {
            if ("pending".equals(schedule.getStatus())) {
                try {
                    long remaining = schedule.getRemainingMillis() / 1000;
                    String timeStr = formatCountdown(remaining);
                    String statusMsg = scheduledModeEnabled ? "✅ (will start automatically)" : "⏸ (waiting for activation)";
                    callback.receiveChatMessage("📅 UPCOMING", 
                        schedule.getItemName() + " starts in " + timeStr + 
                        " (at " + formatDateTime(schedule.getScheduledStartTime()) + ") " + statusMsg);
                } catch (RemoteException e) {
                    // ignore
                }
            }
        }
        
        System.out.println("✅ Client registered: " + clientName);
    }
    
    @Override
    public void unregisterClient(ClientCallback callback) throws RemoteException {
        String clientName = callback.getClientName();
        clients.remove(callback);
        broadcastChatMessage("💬 SYSTEM", clientName + " left.");
        System.out.println("❌ Client left: " + clientName);
    }
    
    @Override
    public void broadcastChatMessage(String sender, String message) throws RemoteException {
        for (ClientCallback client : clients) {
            try {
                client.receiveChatMessage(sender, message);
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
    }
    
    private void broadcastAuctionStart(AuctionItem item) {
        for (ClientCallback client : clients) {
            try {
                client.receiveChatMessage("🎯 AUCTION START", "NOW OPEN: " + item.getName() + " - $" + item.getStartingPrice());
                client.notifyNewAuctionStart(item);
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
    }
    
    private void broadcastBidUpdate(int itemId, double newPrice, String winner) {
        for (ClientCallback client : clients) {
            try {
                client.notifyBidUpdate(itemId, newPrice, winner);
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
    }
    
    // ============ GETTER METHODS ============
    
    @Override
    public List<AuctionItem> getItems() throws RemoteException {
        return new ArrayList<>(items);
    }
    
    @Override
    public String getServerIP() throws RemoteException {
        return serverIP;
    }
    
    @Override
    public List<String> getConnectedClients() throws RemoteException {
        List<String> clientNames = new ArrayList<>();
        for (ClientCallback client : clients) {
            try {
                clientNames.add(client.getClientName());
            } catch (RemoteException e) {
                clients.remove(client);
            }
        }
        return clientNames;
    }
    
    @Override
    public boolean isItemBiddable(int itemId) throws RemoteException {
        if (currentActiveItem == null) return false;
        return currentActiveItem.getId() == itemId && currentActiveItem.isActive() && !currentActiveItem.isSold();
    }
    
    @Override
    public AuctionItem getCurrentActiveItem() throws RemoteException {
        if (currentActiveItem != null && currentActiveItem.isActive() && !currentActiveItem.isSold()) {
            return currentActiveItem;
        }
        return null;
    }
    
    @Override
    public boolean isScheduledModeEnabled() throws RemoteException {
        return scheduledModeEnabled;
    }
    
    private AuctionItem findItemById(int id) {
        for (AuctionItem item : items) {
            if (item.getId() == id) return item;
        }
        return null;
    }
    
    // ============================================================
    // ============ PAYMENT METHODS IMPLEMENTATION ============
    // ============================================================
    
    @Override
    public void createPaymentRecord(int itemId, String winnerName, double amount) throws RemoteException {
        try {
            PaymentRecord existing = DatabaseHelper.getPaymentRecordByItemId(itemId);
            if (existing != null) {
                System.out.println("⚠️ Payment record already exists for item ID: " + itemId);
                return;
            }
            
            AuctionItem item = findItemById(itemId);
            String itemName = item != null ? item.getName() : "Unknown Item";
            
            PaymentRecord payment = new PaymentRecord();
            payment.setItemId(itemId);
            payment.setItemName(itemName);
            payment.setWinnerName(winnerName);
            payment.setAmount(amount);
            payment.setStatus("pending");
            payment.setCreatedAt(new Date());
            payment.setPaymentDate(null);  // Not paid yet
            
            int id = DatabaseHelper.insertPaymentRecord(payment);
            if (id > 0) {
                payment.setId(id);
                paymentRecords.add(payment);
                
                for (ClientCallback client : clients) {
                    try {
                        client.receiveChatMessage("💳 PAYMENT", 
                            "💰 Payment required: " + winnerName + " owes $" + String.format("%.2f", amount) + 
                            " for " + itemName);
                        client.notifyPaymentUpdate(payment);
                    } catch (RemoteException e) {
                        clients.remove(client);
                    }
                }
                
                System.out.println("💳 Payment record created: ID=" + id + 
                                   ", Winner=" + winnerName + 
                                   ", Amount=$" + amount);
            }
            
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public void updatePaymentStatus(int paymentId, String status, String confirmedBy) throws RemoteException {
        try {
            DatabaseHelper.updatePaymentStatus(paymentId, status, confirmedBy);
            
            for (PaymentRecord p : paymentRecords) {
                if (p.getId() == paymentId) {
                    p.setStatus(status);
                    p.setConfirmedBy(confirmedBy);
                    p.setConfirmedAt(new Date());
                    
                    for (ClientCallback client : clients) {
                        try {
                            client.notifyPaymentUpdate(p);
                        } catch (RemoteException e) {
                            clients.remove(client);
                        }
                    }
                    break;
                }
            }
            
            broadcastChatMessage("💳 PAYMENT", "Payment status updated to: " + status);
            
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public void updatePaymentDetails(int paymentId, String paymentMethod, String transactionId, Date paymentDate) throws RemoteException {
        try {
            DatabaseHelper.updatePaymentDetails(paymentId, paymentMethod, transactionId, paymentDate);
            
            for (PaymentRecord p : paymentRecords) {
                if (p.getId() == paymentId) {
                    p.setPaymentMethod(paymentMethod);
                    p.setTransactionId(transactionId);
                    p.setPaymentDate(paymentDate);
                    
                    for (ClientCallback client : clients) {
                        try {
                            client.notifyPaymentUpdate(p);
                        } catch (RemoteException e) {
                            clients.remove(client);
                        }
                    }
                    break;
                }
            }
            
            broadcastChatMessage("💳 PAYMENT", 
                "📝 Payment details updated for ID #" + paymentId + 
                " | Method: " + paymentMethod + 
                " | Transaction: " + transactionId);
            
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentRecord getPaymentById(int paymentId) throws RemoteException {
        try {
            for (PaymentRecord p : paymentRecords) {
                if (p.getId() == paymentId) {
                    return p;
                }
            }
            return DatabaseHelper.getPaymentRecordById(paymentId);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public PaymentRecord getPaymentByItemId(int itemId) throws RemoteException {
        try {
            for (PaymentRecord p : paymentRecords) {
                if (p.getItemId() == itemId) {
                    return p;
                }
            }
            return DatabaseHelper.getPaymentRecordByItemId(itemId);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public List<PaymentRecord> getPaymentsByWinner(String winnerName) throws RemoteException {
        List<PaymentRecord> result = new ArrayList<>();
        for (PaymentRecord p : paymentRecords) {
            if (p.getWinnerName() != null && p.getWinnerName().equals(winnerName)) {
                result.add(p);
            }
        }
        return result;
    }
    
    @Override
    public List<PaymentRecord> getAllPayments() throws RemoteException {
        return new ArrayList<>(paymentRecords);
    }
    
    @Override
    public boolean isPaymentCompleted(int itemId) throws RemoteException {
        try {
            return DatabaseHelper.isPaymentCompleted(itemId);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public void confirmPaymentReceived(int paymentId, String adminName) throws RemoteException {
        try {
            PaymentRecord payment = getPaymentById(paymentId);
            if (payment == null) {
                throw new RemoteException("Payment record not found with ID: " + paymentId);
            }
            
            DatabaseHelper.updatePaymentStatus(paymentId, "completed", adminName);
            
            for (PaymentRecord p : paymentRecords) {
                if (p.getId() == paymentId) {
                    p.setStatus("completed");
                    p.setConfirmedBy(adminName);
                    p.setConfirmedAt(new Date());
                    
                    for (ClientCallback client : clients) {
                        try {
                            client.notifyPaymentUpdate(p);
                        } catch (RemoteException e) {
                            clients.remove(client);
                        }
                    }
                    break;
                }
            }
            
            String msg = "✅ Payment CONFIRMED by " + adminName + " for " + 
                        payment.getItemName() + " ($" + String.format("%.2f", payment.getAmount()) + ")";
            
            broadcastChatMessage("💳 CONFIRMED", msg);
            
            for (ClientCallback client : clients) {
                try {
                    String clientName = client.getClientName();
                    if (clientName != null && clientName.equals(payment.getWinnerName())) {
                        client.receiveChatMessage("🎉 CONGRATULATIONS", 
                            "🎊 Your payment has been confirmed!\n" +
                            "📦 Item: " + payment.getItemName() + "\n" +
                            "💰 Amount: $" + String.format("%.2f", payment.getAmount()) + "\n" +
                            "✅ Status: Ready for delivery!");
                    }
                } catch (RemoteException e) {
                    clients.remove(client);
                }
            }
            
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public double[] getPaymentStatistics() throws RemoteException {
        try {
            double totalAmount = DatabaseHelper.getTotalPaymentAmount();
            int pendingCount = DatabaseHelper.getPaymentCountByStatus("pending");
            int processingCount = DatabaseHelper.getPaymentCountByStatus("processing");
            int completedCount = DatabaseHelper.getPaymentCountByStatus("completed");
            int failedCount = DatabaseHelper.getPaymentCountByStatus("failed");
            
            return new double[]{
                totalAmount,
                pendingCount,
                processingCount,
                completedCount,
                failedCount
            };
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // ============ PAYMENT HISTORY METHODS ============
    // ============================================================
    
    @Override
    public List<String[]> getPaymentHistory() throws RemoteException {
        try {
            return DatabaseHelper.getPaymentHistory();
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public List<String[]> getPaymentHistoryByItem(int itemId) throws RemoteException {
        try {
            return DatabaseHelper.getPaymentHistoryByItem(itemId);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    @Override
    public List<String[]> getPaymentHistoryByWinner(String winnerName) throws RemoteException {
        try {
            return DatabaseHelper.getPaymentHistoryByWinner(winnerName);
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
    
    // ============================================================
    // ============ CLIENT COMPLETE PAYMENT (FIXED) ============
    // ============================================================
    
    @Override
    public void completePaymentByClient(int paymentId, String winnerName, String paymentMethod, String transactionId) throws RemoteException {
        try {
            // ============ STEP 1: Get existing payment record ============
            PaymentRecord existingPayment = getPaymentById(paymentId);
            
            // ============ STEP 2: Determine payment date ============
            Date paymentDate = null;
            boolean isFirstPayment = false;
            
            if (existingPayment != null && existingPayment.getPaymentDate() != null) {
                // ============ FIX: Keep existing payment_date (don't change it) ============
                paymentDate = existingPayment.getPaymentDate();
                System.out.println("📅 Using existing payment_date: " + paymentDate + " (NOT CHANGING)");
            } else {
                // ============ First time payment - set to current date ============
                paymentDate = new Date();
                isFirstPayment = true;
                System.out.println("📅 Setting new payment_date (first payment): " + paymentDate);
            }
            
            // ============ STEP 3: Update payment details ============
            // Only pass paymentDate if it's the first payment
            if (isFirstPayment) {
                // Save payment_date for first payment
                DatabaseHelper.updatePaymentDetails(paymentId, paymentMethod, transactionId, paymentDate);
            } else {
                // Don't update payment_date - keep existing
                DatabaseHelper.updatePaymentDetails(paymentId, paymentMethod, transactionId, null);
            }
            
            // ============ STEP 4: Update payment status to completed ============
            DatabaseHelper.updatePaymentStatus(paymentId, "completed", winnerName);
            
            // ============ STEP 5: Update in-memory list ============
            for (PaymentRecord p : paymentRecords) {
                if (p.getId() == paymentId) {
                    p.setStatus("completed");
                    p.setConfirmedBy(winnerName);
                    p.setConfirmedAt(new Date());
                    p.setPaymentMethod(paymentMethod);
                    p.setTransactionId(transactionId);
                    // Keep existing payment_date if already set
                    if (p.getPaymentDate() == null && isFirstPayment) {
                        p.setPaymentDate(paymentDate);
                    }
                    System.out.println("✅ Updated in-memory payment: ID=" + paymentId + 
                                       ", Date=" + p.getPaymentDate() + 
                                       ", FirstPayment=" + isFirstPayment);
                    break;
                }
            }
            
            // Broadcast to all clients
            broadcastChatMessage("💳 PAYMENT", 
                "✅ Payment completed by " + winnerName + " (Payment ID: " + paymentId + ")");
            
            System.out.println("✅ Payment completed: ID=" + paymentId + ", Winner=" + winnerName + 
                               ", Payment Date=" + paymentDate + ", FirstPayment=" + isFirstPayment);
            
        } catch (SQLException e) {
            throw new RemoteException("Database error: " + e.getMessage());
        }
    }
}