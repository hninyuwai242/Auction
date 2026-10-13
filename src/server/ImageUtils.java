package server;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageUtils {
    
    public static byte[] loadImageBytes(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    return imageToBytes(img);
                }
            }
            
            String userDir = System.getProperty("user.dir");
            String[] basePaths = {
                "images/" + imagePath,
                "./images/" + imagePath,
                "../images/" + imagePath,
                userDir + "/images/" + imagePath,
                "src/images/" + imagePath,
                "./src/images/" + imagePath,
                userDir + "/src/images/" + imagePath,
                "New/images/" + imagePath,
                "./New/images/" + imagePath,
                userDir + "/New/images/" + imagePath
            };
            
            for (String path : basePaths) {
                File f = new File(path);
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        return imageToBytes(img);
                    }
                }
            }
            
            String[] resourcePaths = {
                "/images/" + imagePath,
                "images/" + imagePath,
                "/New/images/" + imagePath,
                "New/images/" + imagePath
            };
            
            for (String path : resourcePaths) {
                java.net.URL url = ImageUtils.class.getResource(path);
                if (url != null) {
                    BufferedImage img = ImageIO.read(url);
                    if (img != null) {
                        return imageToBytes(img);
                    }
                }
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    public static byte[] imageToBytes(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
    
    public static ImageIcon bytesToImageIcon(byte[] imageData, int width, int height) {
        if (imageData == null || imageData.length == 0) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img != null) {
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
    
    public static ImageIcon getItemImage(AuctionItem item, int width, int height) {
        if (item != null && item.hasImage()) {
            ImageIcon icon = bytesToImageIcon(item.getImageData(), width, height);
            if (icon != null && icon.getImage() != null) {
                return icon;
            }
        }
        
        if (item != null && item.getImageFileName() != null && !item.getImageFileName().isEmpty()) {
            byte[] imageData = loadImageBytes(item.getImageFileName());
            if (imageData != null && imageData.length > 0) {
                ImageIcon icon = bytesToImageIcon(imageData, width, height);
                if (icon != null && icon.getImage() != null) {
                    item.setImageData(imageData);
                    return icon;
                }
            }
        }
        
        if (item != null) {
            return createBeautifulImage(item.getIcon(), item.getName(), width, height);
        }
        
        return createPlaceholderImage(width, height);
    }
    
    public static ImageIcon createBeautifulImage(String icon, String itemName, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        Color color1 = getColorFromName(itemName, 0);
        Color color2 = getColorFromName(itemName, 1);
        GradientPaint gradient = new GradientPaint(0, 0, color1, width, height, color2);
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(2, 2, width-5, height-5, 12, 12);
        
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(3, 3, width-7, height-7, 10, 10);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, width / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String displayIcon = icon != null && !icon.isEmpty() ? icon : getDefaultIcon(itemName);
        int x = (width - fm.stringWidth(displayIcon)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(displayIcon, x, y);
        
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillOval(width-15, height-15, 8, 8);
        g2d.fillOval(7, 7, 5, 5);
        
        g2d.dispose();
        return new ImageIcon(img);
    }
    
    public static ImageIcon createPlaceholderImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gradient = new GradientPaint(0, 0, new Color(60, 60, 80), 
                                                   width, height, new Color(40, 40, 60));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 15, 15);
        
        g2d.setColor(new Color(255, 215, 0, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(2, 2, width-5, height-5, 12, 12);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, width / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String displayText = "🖼";
        int x = (width - fm.stringWidth(displayText)) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(displayText, x, y);
        
        g2d.dispose();
        return new ImageIcon(img);
    }
    
    private static Color getColorFromName(String name, int shade) {
        int hash = name != null ? name.hashCode() : 0;
        float hue = (Math.abs(hash) % 360) / 360f;
        if (shade == 0) {
            return Color.getHSBColor(hue, 0.7f, 0.9f);
        } else {
            return Color.getHSBColor(hue, 0.8f, 0.6f);
        }
    }
    
    private static String getDefaultIcon(String name) {
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
    
    public static byte[] createDefaultImageBytes(String text) {
        try {
            int width = 100, height = 100;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            GradientPaint gradient = new GradientPaint(0, 0, new Color(70, 130, 200), 
                                                       width, height, new Color(100, 100, 150));
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, width, height, 15, 15);
            
            g2d.setColor(new Color(255, 215, 0));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(2, 2, width-5, height-5, 12, 12);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 55));
            FontMetrics fm = g2d.getFontMetrics();
            String displayText = text != null && text.length() > 0 ? text.substring(0, 1).toUpperCase() : "?";
            int x = (width - fm.stringWidth(displayText)) / 2;
            int y = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(displayText, x, y);
            
            g2d.dispose();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            return null;
        }
    }
}