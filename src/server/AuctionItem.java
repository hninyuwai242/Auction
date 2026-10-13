package server;

import java.io.Serializable;

public class AuctionItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int id;
    private String name;
    private String description;
    private double currentPrice;
    private double startingPrice;
    private String currentWinner;
    private long endTime;
    private String icon;
    private byte[] imageData;
    private String imageFileName;
    private boolean sold;
    private String winnerName;
    private boolean active;
    private String detailedInfo;
    
    private static final long DEFAULT_DURATION_SECONDS = 120;
    
    public AuctionItem(int id, String name, String description, double startingPrice, byte[] imageData, String imageFileName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.currentWinner = "";
        this.endTime = System.currentTimeMillis() + (DEFAULT_DURATION_SECONDS * 1000);
        this.icon = getIconForItem(name);
        this.imageData = imageData;
        this.imageFileName = imageFileName;
        this.sold = false;
        this.winnerName = "";
        this.active = false;
        this.detailedInfo = getDetailedInfoForItem(name);
    }
    
    public AuctionItem(int id, String name, String description, double startingPrice, long durationSeconds, byte[] imageData, String imageFileName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.currentWinner = "";
        this.endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        this.icon = getIconForItem(name);
        this.imageData = imageData;
        this.imageFileName = imageFileName;
        this.sold = false;
        this.winnerName = "";
        this.active = false;
        this.detailedInfo = getDetailedInfoForItem(name);
    }
    
    private String getIconForItem(String name) {
        if (name == null) return "🖼";
        if (name.contains("Watch") || name.contains("Rolex") || name.contains("Omega")) return "⌚";
        if (name.contains("Laptop") || name.contains("MacBook")) return "💻";
        if (name.contains("Painting")) return "🎨";
        if (name.contains("Guitar") || name.contains("Fender")) return "🎸";
        if (name.contains("Phone") || name.contains("iPhone")) return "📱";
        if (name.contains("Camera") || name.contains("Canon") || name.contains("GoPro")) return "📷";
        if (name.contains("Shoes") || name.contains("Nike")) return "👟";
        if (name.contains("Bag") || name.contains("Louis")) return "👜";
        if (name.contains("Headphones") || name.contains("Sony") || name.contains("Bose")) return "🎧";
        if (name.contains("Drone") || name.contains("DJI")) return "🚁";
        if (name.contains("Bicycle") || name.contains("Trek")) return "🚲";
        if (name.contains("PlayStation") || name.contains("Xbox")) return "🎮";
        if (name.contains("Vacuum") || name.contains("Dyson")) return "🧹";
        if (name.contains("Quest") || name.contains("VR")) return "🕶️";
        if (name.contains("Samsung") || name.contains("Galaxy")) return "📱";
        return "🖼";
    }
    
    private String getDetailedInfoForItem(String name) {
        if (name == null) return "";
        
        if (name.contains("iPhone 15 Pro Max")) {
            return "📱 Specifications:\n• 6.7-inch Super Retina XDR display\n• A17 Pro chip with 6-core GPU\n• 512GB Storage\n• 48MP Main + 12MP Ultra Wide + 12MP Telephoto\n• Titanium frame\n• USB-C port\n• Up to 29 hours video playback";
        }
        if (name.contains("iPhone") && name.contains("15")) {
            return "📱 Specifications:\n• 6.1-inch Super Retina XDR display\n• A16 Bionic chip\n• 128GB/256GB Storage\n• 48MP Main + 12MP Ultra Wide\n• USB-C port\n• Up to 20 hours video playback";
        }
        if (name.contains("MacBook Pro M3")) {
            return "💻 Specifications:\n• 16-inch Liquid Retina XDR display\n• M3 Max chip with 16-core GPU\n• 64GB Unified Memory\n• 2TB SSD Storage\n• 22 hours battery life\n• 3 Thunderbolt 4 ports\n• Space Black color";
        }
        if (name.contains("MacBook Air")) {
            return "💻 Specifications:\n• 13.6-inch Liquid Retina display\n• M2 chip with 8-core GPU\n• 16GB Unified Memory\n• 512GB SSD Storage\n• 18 hours battery life\n• 2 Thunderbolt ports\n• Midnight color";
        }
        if (name.contains("Rolex")) {
            return "⌚ Specifications:\n• 1950s Vintage\n• Automatic movement\n• 18K Yellow Gold case\n• Black dial with gold markers\n• Leather strap\n• Excellent condition\n• Original box and papers included";
        }
        if (name.contains("Omega Seamaster")) {
            return "⌚ Specifications:\n• Diver 300M\n• Automatic movement (Caliber 8800)\n• Ceramic bezel\n• Stainless steel case\n• Date display\n• 300m water resistance\n• 5-year warranty";
        }
        if (name.contains("Apple Watch Ultra 2")) {
            return "⌚ Specifications:\n• 49mm Titanium case\n• Always-On Retina display (3000 nits)\n• GPS + Cellular\n• Fitness tracking\n• Heart rate monitor\n• Blood oxygen sensor\n• Up to 36 hours battery life";
        }
        if (name.contains("Painting")) {
            return "🎨 Specifications:\n• Hand-painted original\n• Abstract Expressionism\n• Oil on canvas\n• 24x36 inches\n• Signed by artist\n• Certificate of Authenticity\n• Gallery-wrapped edges";
        }
        if (name.contains("Fender Stratocaster")) {
            return "🎸 Specifications:\n• 1965 Vintage\n• Original pickups\n• Sunburst finish\n• Maple neck\n• Rosewood fingerboard\n• Original case included\n• Mint condition";
        }
        if (name.contains("Canon EOS R5")) {
            return "📷 Specifications:\n• 45MP Full-frame sensor\n• 8K video recording\n• RF 24-105mm lens\n• 5-axis image stabilization\n• Dual card slots\n• Weather-sealed body\n• Bluetooth & WiFi";
        }
        if (name.contains("GoPro Hero 12")) {
            return "📷 Specifications:\n• 5.3K video at 60fps\n• 27MP photos\n• Waterproof to 10m\n• HyperSmooth 6.0 stabilization\n• Front LCD display\n• Built-in mounting fingers\n• 4K 120fps slow-motion";
        }
        if (name.contains("Nike Air Jordan 1")) {
            return "👟 Specifications:\n• Limited Edition\n• Size 10 US\n• Chicago colorway\n• Never worn\n• Original box\n• Authenticity card\n• 100% genuine leather";
        }
        if (name.contains("Louis Vuitton")) {
            return "👜 Specifications:\n• Authentic Louis Vuitton\n• Monogram canvas\n• Limited Edition\n• Excellent condition\n• Gold hardware\n• Original dust bag\n• Serial number included";
        }
        if (name.contains("Sony WH-1000XM5")) {
            return "🎧 Specifications:\n• Industry-leading noise cancellation\n• 30 hours battery life\n• Quick charge (3 min = 3 hours)\n• 4.2 Bluetooth\n• LDAC support\n• Foldable design\n• Carrying case included";
        }
        if (name.contains("Bose QuietComfort 45")) {
            return "🎧 Specifications:\n• Noise cancelling\n• 24-hour battery life\n• 2.5-hour full charge\n• TriPort acoustic design\n• Volume-optimized EQ\n• Lightweight (240g)\n• Carry case included";
        }
        if (name.contains("DJI Mavic 3")) {
            return "🚁 Specifications:\n• Pro Cine Premium Combo\n• 3 batteries included\n• 4/3 CMOS Hasselblad camera\n• 5.1K video\n• 46-minute flight time\n• Hard case included\n• 3 batteries";
        }
        if (name.contains("Samsung Galaxy S24 Ultra")) {
            return "📱 Specifications:\n• 6.8-inch Dynamic AMOLED 2X\n• Snapdragon 8 Gen 3\n• 1TB Storage\n• 200MP Main camera\n• 12GB RAM\n• S Pen included\n• 5000mAh battery";
        }
        if (name.contains("PlayStation 5 Pro")) {
            return "🎮 Specifications:\n• Ultra HD Blu-ray drive\n• 2TB SSD Storage\n• DualSense Wireless Controller\n• 8K gaming support\n• Ray tracing\n• 3D Audio\n• 2-year warranty";
        }
        if (name.contains("Xbox Series X")) {
            return "🎮 Specifications:\n• 1TB SSD Storage\n• 4K gaming at 120fps\n• Quick Resume\n• Dolby Atmos\n• 12 Teraflops GPU\n• 3.8GHz CPU\n• 2-year warranty";
        }
        if (name.contains("Dyson V15")) {
            return "🧹 Specifications:\n• Digital Motor\n• Laser Dust Detection\n• 60-minute runtime\n• 2.3kg weight\n• 6 accessories\n• 5-stage filtration\n• 2-year warranty";
        }
        if (name.contains("Trek Mountain Bike")) {
            return "🚲 Specifications:\n• Carbon frame\n• Full suspension\n• 12-speed gear\n• 29-inch wheels\n• Disc brakes\n• 1.2kg frame weight\n• 5-year warranty";
        }
        if (name.contains("Meta Quest 3")) {
            return "🕶️ Specifications:\n• VR/AR Mixed Reality\n• 8GB RAM\n• 128GB Storage\n• 4K+ Infinite Display\n• Touch Plus Controllers\n• 3D positional audio\n• 2-hour battery life";
        }
        return "✨ High-quality item in excellent condition";
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getStartingPrice() { return startingPrice; }
    public String getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(String currentWinner) { this.currentWinner = currentWinner; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getIcon() { return icon; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
    public String getImageFileName() { return imageFileName; }
    public boolean isSold() { return sold; }
    public void setSold(boolean sold) { this.sold = sold; }
    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public boolean isActive() { return active && System.currentTimeMillis() < endTime; }
    public void setActive(boolean active) { this.active = active; }
    public String getDetailedInfo() { return detailedInfo; }
    public void setDetailedInfo(String detailedInfo) { this.detailedInfo = detailedInfo; }
    
    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }
    
    public int getImageSize() {
        return imageData != null ? imageData.length : 0;
    }
    
    public long getRemainingSeconds() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return remaining > 0 ? remaining : 0;
    }
    
    public String getFormattedRemainingTime() {
        long seconds = getRemainingSeconds();
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    @Override
    public String toString() {
        return name + " - $" + currentPrice;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setImageFileName(String fileName) {
        this.imageFileName = fileName;
    }
}