package client;

import server.AuctionItem;
import server.AuctionService;
import server.ClientCallback;
import server.ImageUtils;
import server.PaymentRecord;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AuctionClientGUI extends JFrame {
    
    private AuctionService auctionService;
    private ClientCallbackImpl callback;
    private String clientName;
    private String serverIP;
    private int serverPort = 1099;
    
    private JPanel mainContentPanel;
    private JPanel currentAuctionPanel;
    private JPanel itemInfoPanel;
    private JPanel bidPanel;
    private JPanel paymentPanel;
    private JPanel itemsListPanel;
    
    private JLabel currentItemNameLabel;
    private JLabel currentItemDescLabel;
    private JLabel currentPriceLabel;
    private JLabel timeLeftLabel;
    private JLabel timeLeftTitleLabel;
    private JLabel startTimeLabel;
    private JLabel currentWinnerLabel;
    private JLabel currentItemImageLabel;
    private JLabel clientNameLabel;
    private JLabel statusLabel;
    private JLabel nextAuctionLabel;
    private JLabel scheduleStatusLabel;
    private JLabel chatCountLabel;
    
    private JButton bidButton;
    private JLabel bidAmountDisplayLabel;
    private int bidIncrement = 100;
    
    private JPanel paymentPanelContainer;
    private JLabel paymentWinnerLabel;
    private JLabel paymentItemLabel;
    private JLabel paymentAmountLabel;
    private JTextField paymentAmountField;
    private JComboBox<String> paymentMethodCombo;
    private JButton paymentCompleteButton;
    
    // Chat Components
    private JTextArea chatArea;
    private JTextField chatInput;
    
    private DefaultListModel<AuctionItem> itemListModel;
    private JList<AuctionItem> itemList;
    
    private Timer refreshTimer;
    private Timer countdownTimer;
    
    private AuctionItem activeAuctionItem;
    private List<AuctionItem> allItems;
    private boolean isAuctionRunning = false;
    private boolean paymentCompleted = false;
    private String currentWinner = "";
    private String lastWinnerName = "";
    
    private String scheduledItemName = "";
    private String scheduledStartTime = "";
    private String scheduledPrice = "";
    private long scheduledRemainingSeconds = 0;
    private boolean hasSchedule = false;
    private boolean auctionStarted = false;
    private boolean scheduledModeEnabled = false;
    
    private static final Color BG_DARK = new Color(10, 15, 25);
    private static final Color BG_CARD = new Color(20, 28, 45);
    private static final Color BG_HOVER = new Color(30, 40, 60);
    private static final Color ACCENT_GOLD = new Color(255, 193, 7);
    private static final Color ACCENT_BLUE = new Color(0, 150, 255);
    private static final Color ACCENT_GREEN = new Color(0, 230, 118);
    private static final Color ACCENT_RED = new Color(255, 70, 70);
    private static final Color ACCENT_PURPLE = new Color(156, 39, 176);
    private static final Color ACCENT_ORANGE = new Color(255, 152, 0);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(180, 190, 210);
    private static final Color PAYMENT_BG = new Color(0, 200, 100, 30);
    
    public AuctionClientGUI() {
        setTitle("🏛 PREMIUM AUCTION HOUSE - CLIENT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 950);
        setLocationRelativeTo(null);
        setBackground(BG_DARK);
        
        showLoginDialog();
        setupUI();
        startTimers();
        connectToServer();
    }
    
    // ==================== LOGIN DIALOG ====================
    
    private void showLoginDialog() {
        JDialog dialog = new JDialog(this, "Login to Auction Server", true);
        dialog.setSize(500, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        JLabel titleLabel = new JLabel("🏛 Connect to Auction Server");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(ACCENT_GOLD);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridy = 1;
        JLabel ipLabel = new JLabel("🌐 Server IP:");
        ipLabel.setForeground(TEXT_PRIMARY);
        ipLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(ipLabel, gbc);
        
        JTextField ipField = new JTextField("localhost", 20);
        ipField.setBackground(new Color(45, 52, 65));
        ipField.setForeground(TEXT_PRIMARY);
        ipField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ipField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        panel.add(ipField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel portLabel = new JLabel("🔌 Port:");
        portLabel.setForeground(TEXT_PRIMARY);
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(portLabel, gbc);
        
        JTextField portField = new JTextField("1099", 20);
        portField.setBackground(new Color(45, 52, 65));
        portField.setForeground(TEXT_PRIMARY);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        portField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        panel.add(portField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel nameLabel = new JLabel("👤 Your Name:");
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(nameLabel, gbc);
        
        JTextField nameField = new JTextField(20);
        nameField.setBackground(new Color(45, 52, 65));
        nameField.setForeground(TEXT_PRIMARY);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(15, 8, 8, 8);
        
        JButton joinBtn = new JButton("🚀 Connect to Auction");
        joinBtn.setBackground(ACCENT_GOLD);
        joinBtn.setForeground(Color.BLACK);
        joinBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        joinBtn.setBorder(new EmptyBorder(12, 50, 12, 50));
        joinBtn.setFocusPainted(false);
        joinBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        panel.add(joinBtn, gbc);
        
        dialog.add(panel, BorderLayout.CENTER);
        
        joinBtn.addActionListener(e -> {
            String ip = ipField.getText().trim();
            String portStr = portField.getText().trim();
            String name = nameField.getText().trim();
            
            if (ip.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "⚠️ Please enter Server IP address!");
                return;
            }
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "⚠️ Please enter your name!");
                return;
            }
            if (name.length() < 2) {
                JOptionPane.showMessageDialog(dialog, "⚠️ Name must be at least 2 characters!");
                return;
            }
            
            try {
                serverIP = ip;
                serverPort = Integer.parseInt(portStr);
                clientName = name;
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "⚠️ Please enter a valid port number!");
            }
        });
        
        nameField.addActionListener(e -> joinBtn.doClick());
        ipField.addActionListener(e -> joinBtn.doClick());
        portField.addActionListener(e -> joinBtn.doClick());
        
        dialog.setVisible(true);
    }
    
    // ==================== SETUP UI ====================
    
    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_DARK);
        
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.35);
        splitPane.setBorder(null);
        splitPane.setBackground(BG_DARK);
        
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();
        
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    // ==================== HEADER PANEL ====================
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 25, 45), 
                                                      getWidth(), 0, new Color(30, 40, 65));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setBorder(new EmptyBorder(15, 25, 15, 25));
        header.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🏛 PREMIUM AUCTION HOUSE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        clientNameLabel = new JLabel("👤 " + clientName);
        clientNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        clientNameLabel.setForeground(ACCENT_GOLD);
        
        statusLabel = new JLabel("🔄 Connecting...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(ACCENT_GREEN);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);
        
        JLabel serverLabel = new JLabel("🌐 " + serverIP + ":" + serverPort);
        serverLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        serverLabel.setForeground(TEXT_SECONDARY);
        
        rightPanel.add(serverLabel);
        rightPanel.add(clientNameLabel);
        rightPanel.add(statusLabel);
        
        header.add(titleLabel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        
        return header;
    }
    
    // ==================== LEFT PANEL - ITEMS LIST ====================
    
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(BG_DARK);
        
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setBackground(BG_CARD);
        itemsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel itemsTitle = new JLabel("📦 AUCTION ITEMS");
        itemsTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        itemsTitle.setForeground(ACCENT_GOLD);
        itemsPanel.add(itemsTitle, BorderLayout.NORTH);
        
        itemListModel = new DefaultListModel<>();
        itemList = new JList<>(itemListModel);
        itemList.setBackground(new Color(35, 42, 55));
        itemList.setForeground(TEXT_SECONDARY);
        itemList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemList.setCellRenderer(new ItemListRenderer());
        itemList.setBorder(new EmptyBorder(5, 5, 5, 5));
        itemList.setFixedCellHeight(70);
        
        JScrollPane scrollPane = new JScrollPane(itemList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(35, 42, 55));
        
        itemsPanel.add(scrollPane, BorderLayout.CENTER);
        leftPanel.add(itemsPanel, BorderLayout.CENTER);
        
        return leftPanel;
    }
    
    // ==================== RIGHT PANEL ====================
    
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(BG_DARK);
        
        // ========== CURRENT AUCTION PANEL ==========
        currentAuctionPanel = new JPanel(new BorderLayout(10, 10));
        currentAuctionPanel.setBackground(BG_CARD);
        currentAuctionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_GOLD, 2),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // ========== AUCTION HEADER ==========
        JPanel auctionHeader = new JPanel(new BorderLayout());
        auctionHeader.setOpaque(false);
        
        JLabel auctionTitle = new JLabel("🔥 CURRENT AUCTION");
        auctionTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        auctionTitle.setForeground(ACCENT_GOLD);
        auctionHeader.add(auctionTitle, BorderLayout.WEST);
        
        // Time Left Panel
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        timePanel.setOpaque(false);
        
        JLabel timeIconLabel = new JLabel("⏱️");
        timeIconLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        timeIconLabel.setForeground(ACCENT_RED);
        
        timeLeftLabel = new JLabel("--:--");
        timeLeftLabel.setFont(new Font("Segoe UI", Font.BOLD, 56));
        timeLeftLabel.setForeground(TEXT_SECONDARY);
        
        startTimeLabel = new JLabel("");
        startTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        startTimeLabel.setForeground(TEXT_SECONDARY);
        
        JPanel timeContainer = new JPanel(new BorderLayout());
        timeContainer.setOpaque(false);
        
        JPanel timeTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        timeTopPanel.setOpaque(false);
        timeTopPanel.add(timeIconLabel);
        timeTopPanel.add(timeLeftLabel);
        
        timeContainer.add(timeTopPanel, BorderLayout.NORTH);
        timeContainer.add(startTimeLabel, BorderLayout.SOUTH);
        
        timePanel.add(timeContainer);
        auctionHeader.add(timePanel, BorderLayout.EAST);
        
        currentAuctionPanel.add(auctionHeader, BorderLayout.NORTH);
        
        // ========== SCHEDULE STATUS ==========
        JPanel scheduleStatusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        scheduleStatusPanel.setOpaque(false);
        scheduleStatusPanel.setBackground(new Color(ACCENT_ORANGE.getRed(), ACCENT_ORANGE.getGreen(), ACCENT_ORANGE.getBlue(), 30));
        scheduleStatusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_ORANGE, 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        
        scheduleStatusLabel = new JLabel("📅 No schedule available - Please wait for admin");
        scheduleStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scheduleStatusLabel.setForeground(TEXT_SECONDARY);
        scheduleStatusPanel.add(scheduleStatusLabel);
        
        currentAuctionPanel.add(scheduleStatusPanel, BorderLayout.NORTH);
        
        // ========== ITEM INFO PANEL ==========
        itemInfoPanel = new JPanel(new BorderLayout(15, 10));
        itemInfoPanel.setOpaque(false);
        itemInfoPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        // Image - LEFT Side
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(250, 250));
        
        currentItemImageLabel = new JLabel();
        currentItemImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentItemImageLabel.setVerticalAlignment(SwingConstants.CENTER);
        currentItemImageLabel.setPreferredSize(new Dimension(250, 250));
        currentItemImageLabel.setBorder(BorderFactory.createLineBorder(new Color(80, 90, 110), 2));
        currentItemImageLabel.setBackground(new Color(30, 38, 55));
        currentItemImageLabel.setOpaque(true);
        
        currentItemImageLabel.setText("🖼️");
        currentItemImageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        currentItemImageLabel.setForeground(TEXT_SECONDARY);
        currentItemImageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        currentItemImageLabel.setVerticalTextPosition(SwingConstants.CENTER);
        
        imagePanel.add(currentItemImageLabel, BorderLayout.CENTER);
        itemInfoPanel.add(imagePanel, BorderLayout.WEST);
        
        // Details - RIGHT Side
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Item Name
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        currentItemNameLabel = new JLabel("⏳ Waiting for auction...");
        currentItemNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        currentItemNameLabel.setForeground(TEXT_PRIMARY);
        detailsPanel.add(currentItemNameLabel, gbc);
        
        // Next Auction Label
        gbc.gridy = 1;
        nextAuctionLabel = new JLabel("");
        nextAuctionLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nextAuctionLabel.setForeground(ACCENT_ORANGE);
        detailsPanel.add(nextAuctionLabel, gbc);
        
        // Description
        gbc.gridy = 2;
        currentItemDescLabel = new JLabel("No auction scheduled yet");
        currentItemDescLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        currentItemDescLabel.setForeground(TEXT_SECONDARY);
        detailsPanel.add(currentItemDescLabel, gbc);
        
        // Price
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pricePanel.setOpaque(false);
        JLabel priceLabel = new JLabel("💰 Current Price: ");
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        priceLabel.setForeground(ACCENT_GOLD);
        currentPriceLabel = new JLabel("$0.00");
        currentPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        currentPriceLabel.setForeground(ACCENT_GREEN);
        pricePanel.add(priceLabel);
        pricePanel.add(currentPriceLabel);
        detailsPanel.add(pricePanel, gbc);
        
        // Winner
        gbc.gridy = 4;
        JPanel winnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        winnerPanel.setOpaque(false);
        JLabel winnerLabel = new JLabel("🏆 Current Winner: ");
        winnerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        winnerLabel.setForeground(ACCENT_GOLD);
        currentWinnerLabel = new JLabel("No bids");
        currentWinnerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        currentWinnerLabel.setForeground(ACCENT_BLUE);
        winnerPanel.add(winnerLabel);
        winnerPanel.add(currentWinnerLabel);
        detailsPanel.add(winnerPanel, gbc);
        
        // Time Left Title (Winner အောက်မှာ)
        gbc.gridy = 5;
        JPanel timeLeftTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timeLeftTitlePanel.setOpaque(false);
        timeLeftTitleLabel = new JLabel("⏱️ Time Left: ");
        timeLeftTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timeLeftTitleLabel.setForeground(ACCENT_GOLD);
        JLabel timeLeftValue = new JLabel("--:--");
        timeLeftValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timeLeftValue.setForeground(TEXT_SECONDARY);
        timeLeftTitlePanel.add(timeLeftTitleLabel);
        timeLeftTitlePanel.add(timeLeftValue);
        detailsPanel.add(timeLeftTitlePanel, gbc);
        
        itemInfoPanel.add(detailsPanel, BorderLayout.CENTER);
        
        // ========== BID PANEL ==========
        bidPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bidPanel.setOpaque(false);
        bidPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 60, 80)),
            new EmptyBorder(10, 0, 5, 0)
        ));
        
        JLabel bidLabel = new JLabel("💰 Bid Amount: ");
        bidLabel.setForeground(TEXT_PRIMARY);
        bidLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        bidAmountDisplayLabel = new JLabel("$0");
        bidAmountDisplayLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        bidAmountDisplayLabel.setForeground(ACCENT_GOLD);
        
        bidButton = new JButton("🚀 Place Bid (+$100)");
        bidButton.setBackground(ACCENT_GOLD);
        bidButton.setForeground(Color.BLACK);
        bidButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bidButton.setBorder(new EmptyBorder(12, 35, 12, 35));
        bidButton.setFocusPainted(false);
        bidButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bidButton.setEnabled(false);
        
        bidButton.addActionListener(e -> placeBid());
        
        JLabel incrementLabel = new JLabel("(+$100 each click)");
        incrementLabel.setForeground(TEXT_SECONDARY);
        incrementLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        
        bidPanel.add(bidLabel);
        bidPanel.add(bidAmountDisplayLabel);
        bidPanel.add(Box.createHorizontalStrut(5));
        bidPanel.add(bidButton);
        bidPanel.add(Box.createHorizontalStrut(5));
        bidPanel.add(incrementLabel);
        
        // ========== CENTER PANEL ==========
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(itemInfoPanel, BorderLayout.CENTER);
        centerPanel.add(bidPanel, BorderLayout.SOUTH);
        
        currentAuctionPanel.add(centerPanel, BorderLayout.CENTER);
        
        // ========== PAYMENT PANEL ==========
        paymentPanelContainer = new JPanel(new BorderLayout(10, 10));
        paymentPanelContainer.setBackground(PAYMENT_BG);
        paymentPanelContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_GREEN, 2),
            new EmptyBorder(15, 20, 15, 20)
        ));
        paymentPanelContainer.setVisible(false);
        
        JLabel paymentTitle = new JLabel("💳 PAYMENT DETAILS");
        paymentTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        paymentTitle.setForeground(ACCENT_GREEN);
        paymentPanelContainer.add(paymentTitle, BorderLayout.NORTH);
        
        JPanel paymentInfoPanel = new JPanel(new GridLayout(7, 1, 5, 8));
        paymentInfoPanel.setOpaque(false);
        paymentInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        paymentWinnerLabel = new JLabel("👤 Winner: ");
        paymentWinnerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentWinnerLabel.setForeground(TEXT_PRIMARY);
        paymentInfoPanel.add(paymentWinnerLabel);
        
        paymentItemLabel = new JLabel("📦 Item: ");
        paymentItemLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentItemLabel.setForeground(TEXT_PRIMARY);
        paymentInfoPanel.add(paymentItemLabel);
        
        JPanel amountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        amountPanel.setOpaque(false);
        JLabel amountLabel = new JLabel("💰 Amount ($): ");
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        amountLabel.setForeground(TEXT_PRIMARY);
        
        paymentAmountField = new JTextField(15);
        paymentAmountField.setBackground(new Color(45, 52, 65));
        paymentAmountField.setForeground(ACCENT_GOLD);
        paymentAmountField.setFont(new Font("Segoe UI", Font.BOLD, 18));
        paymentAmountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        paymentAmountField.setEditable(true);
        
        amountPanel.add(amountLabel);
        amountPanel.add(paymentAmountField);
        paymentInfoPanel.add(amountPanel);
        
        JPanel methodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        methodPanel.setOpaque(false);
        JLabel methodLabel = new JLabel("💳 Payment Method: ");
        methodLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        methodLabel.setForeground(TEXT_PRIMARY);
        
        String[] paymentMethods = {
            "🏦 Bank Transfer (KBZ/AYA/CB)",
            "📱 Wave Money",
            "📱 KBZ Pay",
            "📱 AYA Pay",
            "💵 Cash (On Delivery)",
            "💳 Credit/Debit Card"
        };
        paymentMethodCombo = new JComboBox<>(paymentMethods);
        paymentMethodCombo.setBackground(new Color(45, 52, 65));
        paymentMethodCombo.setForeground(TEXT_PRIMARY);
        paymentMethodCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        paymentMethodCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        methodPanel.add(methodLabel);
        methodPanel.add(paymentMethodCombo);
        paymentInfoPanel.add(methodPanel);
        
        JPanel paymentButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        paymentButtonPanel.setOpaque(false);
        
        paymentCompleteButton = new JButton("✅ Complete Payment");
        paymentCompleteButton.setBackground(ACCENT_GREEN);
        paymentCompleteButton.setForeground(Color.BLACK);
        paymentCompleteButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentCompleteButton.setBorder(new EmptyBorder(10, 30, 10, 30));
        paymentCompleteButton.setFocusPainted(false);
        paymentCompleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        paymentCompleteButton.setEnabled(true);
        paymentCompleteButton.addActionListener(e -> completePayment());
        
        JButton paymentCancelButton = new JButton("❌ Cancel");
        paymentCancelButton.setBackground(new Color(108, 117, 125));
        paymentCancelButton.setForeground(Color.WHITE);
        paymentCancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentCancelButton.setBorder(new EmptyBorder(10, 30, 10, 30));
        paymentCancelButton.setFocusPainted(false);
        paymentCancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        paymentCancelButton.addActionListener(e -> hidePaymentPanel());
        
        paymentButtonPanel.add(paymentCompleteButton);
        paymentButtonPanel.add(paymentCancelButton);
        paymentInfoPanel.add(paymentButtonPanel);
        
        paymentPanelContainer.add(paymentInfoPanel, BorderLayout.CENTER);
        
        currentAuctionPanel.add(paymentPanelContainer, BorderLayout.SOUTH);
        
        // ========== LIVE CHAT PANEL ==========
        JPanel chatPanel = createChatPanel();
        chatPanel.setPreferredSize(new Dimension(0, 200));
        
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setTopComponent(currentAuctionPanel);
        rightSplitPane.setBottomComponent(chatPanel);
        rightSplitPane.setResizeWeight(0.75);
        rightSplitPane.setBorder(null);
        rightSplitPane.setBackground(BG_DARK);
        
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);
        
        return rightPanel;
    }
    
    // ==================== CHAT PANEL ====================
    
    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(BG_CARD);
        chatPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setOpaque(false);
        
        JLabel chatTitle = new JLabel("💬 LIVE CHAT");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chatTitle.setForeground(ACCENT_GOLD);
        chatHeader.add(chatTitle, BorderLayout.WEST);
        
        chatCountLabel = new JLabel("👥 0 online");
        chatCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        chatCountLabel.setForeground(TEXT_SECONDARY);
        chatHeader.add(chatCountLabel, BorderLayout.EAST);
        
        chatPanel.add(chatHeader, BorderLayout.NORTH);
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(22, 28, 38));
        chatArea.setForeground(new Color(180, 190, 205));
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBorder(new EmptyBorder(5, 8, 5, 8));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(new Color(22, 28, 38));
        chatScroll.setPreferredSize(new Dimension(0, 120));
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        
        JPanel chatInputPanel = new JPanel(new BorderLayout(5, 5));
        chatInputPanel.setOpaque(false);
        chatInputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        chatInput = new JTextField();
        chatInput.setBackground(new Color(45, 52, 65));
        chatInput.setForeground(TEXT_PRIMARY);
        chatInput.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chatInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        chatInput.addActionListener(e -> sendChatMessage());
        
        JButton sendButton = new JButton("📤");
        sendButton.setBackground(ACCENT_BLUE);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.setBorder(new EmptyBorder(5, 12, 5, 12));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendChatMessage());
        
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(chatInputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    // ==================== BOTTOM PANEL ====================
    
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        bottomPanel.setBackground(BG_DARK);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(45, 52, 65)));
        
        JLabel status = new JLabel("💡 Connected to auction server | Click 'Place Bid (+$100)' to bid");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(TEXT_SECONDARY);
        bottomPanel.add(status);
        
        return bottomPanel;
    }
    
    // ==================== ITEM LIST RENDERER ====================
    
    private class ItemListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                      boolean isSelected, boolean cellHasFocus) {
            
            JPanel panel = new JPanel(new BorderLayout(10, 5));
            panel.setOpaque(true);
            panel.setBorder(new EmptyBorder(8, 10, 8, 10));
            
            if (isSelected) {
                panel.setBackground(new Color(0, 150, 255, 80));
            } else {
                panel.setBackground(index % 2 == 0 ? new Color(20, 28, 45) : new Color(28, 35, 48));
            }
            
            if (value instanceof AuctionItem) {
                AuctionItem item = (AuctionItem) value;
                
                JLabel imageLabel = new JLabel();
                imageLabel.setPreferredSize(new Dimension(50, 50));
                imageLabel.setHorizontalAlignment(JLabel.CENTER);
                
                ImageIcon icon = ImageUtils.getItemImage(item, 50, 50);
                if (icon != null) {
                    imageLabel.setIcon(icon);
                    imageLabel.setText("");
                } else {
                    imageLabel.setText("🖼");
                    imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                }
                panel.add(imageLabel, BorderLayout.WEST);
                
                JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                textPanel.setOpaque(false);
                
                JLabel nameLabel = new JLabel(item.getName());
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                if (item.isSold()) {
                    nameLabel.setForeground(new Color(150, 100, 100));
                } else if (activeAuctionItem != null && activeAuctionItem.getId() == item.getId()) {
                    nameLabel.setForeground(ACCENT_GOLD);
                } else {
                    nameLabel.setForeground(TEXT_PRIMARY);
                }
                
                JLabel infoLabel = new JLabel();
                infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                String status = item.isSold() ? "✅ SOLD" : 
                               (item.isActive() ? "🔥 OPEN" : "⏳ WAITING");
                String winner = item.isSold() && !item.getWinnerName().isEmpty() ? 
                    " → Winner: " + item.getWinnerName() : "";
                infoLabel.setText("$" + String.format("%.2f", item.getCurrentPrice()) + " | " + status + winner);
                
                if (item.isSold()) {
                    infoLabel.setForeground(new Color(150, 100, 100));
                } else if (item.isActive()) {
                    infoLabel.setForeground(ACCENT_GREEN);
                } else {
                    infoLabel.setForeground(TEXT_SECONDARY);
                }
                
                textPanel.add(nameLabel);
                textPanel.add(infoLabel);
                
                panel.add(textPanel, BorderLayout.CENTER);
            }
            
            return panel;
        }
    }
    
    // ==================== UPDATE CHAT COUNT ====================
    
    private void updateChatCount() {
        try {
            if (auctionService != null) {
                List<String> clients = auctionService.getConnectedClients();
                int count = (clients != null) ? clients.size() : 0;
                SwingUtilities.invokeLater(() -> {
                    chatCountLabel.setText("👥 " + count + " online");
                });
            }
        } catch (RemoteException e) {
            // ignore
        }
    }
    
    // ==================== CONNECT TO SERVER ====================
    
    private void connectToServer() {
        new Thread(() -> {
            try {
                String url = "rmi://" + serverIP + ":" + serverPort + "/AuctionService";
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("🔗 Connecting to " + serverIP + "...");
                });
                
                System.out.println("🔄 Connecting to: " + url);
                auctionService = (AuctionService) Naming.lookup(url);
                
                callback = new ClientCallbackImpl();
                auctionService.registerClient(callback);
                
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("✅ Connected to " + serverIP);
                    statusLabel.setForeground(ACCENT_GREEN);
                    
                    refreshItemList();
                    updateChatCount();
                    
                    try {
                        scheduledModeEnabled = auctionService.isScheduledModeEnabled();
                        updateScheduleStatus();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                
                System.out.println("✅ Connected to auction server: " + url);
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("❌ Connection Failed");
                    statusLabel.setForeground(ACCENT_RED);
                    JOptionPane.showMessageDialog(this, 
                        "❌ Failed to connect to server!\n\n" +
                        "📡 Server: " + serverIP + ":" + serverPort + "\n\n" +
                        "Please check:\n" +
                        "1. Server is running\n" +
                        "2. IP address is correct\n" +
                        "3. Firewall is not blocking port " + serverPort + "\n\n" +
                        "Error: " + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    // ==================== REFRESH ITEMS ====================
    
    private void refreshItemList() {
        try {
            if (auctionService != null) {
                allItems = auctionService.getItems();
                SwingUtilities.invokeLater(() -> {
                    itemListModel.clear();
                    if (allItems != null && !allItems.isEmpty()) {
                        for (AuctionItem item : allItems) {
                            itemListModel.addElement(item);
                        }
                        System.out.println("📦 Loaded " + allItems.size() + " items into list");
                    } else {
                        System.out.println("⚠️ No items found in database");
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Error refreshing items: " + e.getMessage());
        }
    }
    
    // ==================== UPDATE SCHEDULE STATUS ====================
    
    private void updateScheduleStatus() {
        SwingUtilities.invokeLater(() -> {
            if (hasSchedule) {
                if (scheduledModeEnabled) {
                    scheduleStatusLabel.setText("✅ Scheduled Mode: ENABLED - Auction will start at " + scheduledStartTime);
                    scheduleStatusLabel.setForeground(ACCENT_GREEN);
                    scheduleStatusLabel.setBackground(new Color(ACCENT_GREEN.getRed(), ACCENT_GREEN.getGreen(), ACCENT_GREEN.getBlue(), 30));
                } else {
                    scheduleStatusLabel.setText("⏸ Scheduled Mode: DISABLED - Waiting for admin to enable...");
                    scheduleStatusLabel.setForeground(ACCENT_ORANGE);
                    scheduleStatusLabel.setBackground(new Color(ACCENT_ORANGE.getRed(), ACCENT_ORANGE.getGreen(), ACCENT_ORANGE.getBlue(), 30));
                }
            } else {
                scheduleStatusLabel.setText("📅 No schedule available - Please wait for admin");
                scheduleStatusLabel.setForeground(TEXT_SECONDARY);
                scheduleStatusLabel.setBackground(new Color(100, 100, 100, 30));
            }
        });
    }
    
    // ==================== PLACE BID (FIXED) ====================
    
    private void placeBid() {
        if (activeAuctionItem == null) {
            showStatusMessage("⚠️", "No active auction to bid on!");
            return;
        }
        
        try {
            List<AuctionItem> items = auctionService.getItems();
            double currentPrice = activeAuctionItem.getCurrentPrice();
            
            for (AuctionItem item : items) {
                if (item.getId() == activeAuctionItem.getId()) {
                    currentPrice = item.getCurrentPrice();
                    activeAuctionItem.setCurrentPrice(currentPrice);
                    activeAuctionItem.setCurrentWinner(item.getCurrentWinner());
                    break;
                }
            }
            
            double bidAmount = currentPrice + bidIncrement;
            
            System.out.println("🔍 Placing bid: currentPrice=" + currentPrice + ", bidAmount=" + bidAmount);
            System.out.println("   Client: " + clientName + ", Item: " + activeAuctionItem.getName());
            
            boolean success = auctionService.placeBid(activeAuctionItem.getId(), bidAmount, clientName, callback);
            if (success) {
                bidAmountDisplayLabel.setText("$" + (int)bidAmount);
                currentPriceLabel.setText("$" + String.format("%.2f", bidAmount));
                currentWinnerLabel.setText(clientName);
                showStatusMessage("💰", "✅ You placed a bid of $" + String.format("%.2f", bidAmount));
            } else {
                refreshItemList();
                showStatusMessage("⚠️", "Bid was unsuccessful. Please try again.");
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error placing bid: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== SEND CHAT MESSAGE ====================
    
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;
        
        try {
            auctionService.broadcastChatMessage(clientName, message);
            chatInput.setText("");
        } catch (Exception e) {
            addChatMessage("⚠️ SYSTEM", "Failed to send message: " + e.getMessage());
        }
    }
    
    private void addChatMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            
            if (sender.equals("SYSTEM") || sender.equals("⚠️ SYSTEM") || 
                sender.equals("💰 BID") || sender.equals("🏆 RESULT") ||
                sender.equals("🎯 AUCTION") || sender.equals("📅 SYSTEM") ||
                sender.equals("📅 ALERT") || sender.equals("📅 NEXT AUCTION") ||
                sender.equals("💬 SYSTEM") || sender.equals("💳 PAYMENT") ||
                sender.equals("🧾 RECEIPT")) {
                chatArea.append("[" + timestamp + "] " + sender + ": " + message + "\n");
            } else if (sender.equals(clientName)) {
                chatArea.append("[" + timestamp + "] 👤 You: " + message + "\n");
            } else {
                chatArea.append("[" + timestamp + "] " + sender + ": " + message + "\n");
            }
            
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    // ==================== PAYMENT METHODS ====================
    
    private void showPaymentPanel(String winnerName, String itemName, double amount) {
        SwingUtilities.invokeLater(() -> {
            if (activeAuctionItem == null) {
                for (int i = 0; i < itemListModel.getSize(); i++) {
                    AuctionItem item = itemListModel.get(i);
                    if (item.getName().equals(itemName)) {
                        activeAuctionItem = item;
                        break;
                    }
                }
                
                if (activeAuctionItem == null && allItems != null) {
                    for (AuctionItem item : allItems) {
                        if (item.getName().equals(itemName)) {
                            activeAuctionItem = item;
                            break;
                        }
                    }
                }
            }
            
            paymentWinnerLabel.setText("👤 Winner: " + winnerName);
            paymentItemLabel.setText("📦 Item: " + itemName);
            paymentAmountField.setText(String.format("%.2f", amount));
            paymentAmountField.setForeground(ACCENT_GOLD);
            paymentMethodCombo.setSelectedIndex(0);
            currentWinner = winnerName;
            
            paymentPanelContainer.setVisible(true);
            bidPanel.setVisible(false);
            
            paymentCompleted = false;
            paymentCompleteButton.setEnabled(true);
            paymentCompleteButton.setText("✅ Complete Payment");
            
            revalidate();
            repaint();
        });
    }
    
    private void hidePaymentPanel() {
        SwingUtilities.invokeLater(() -> {
            paymentPanelContainer.setVisible(false);
            bidPanel.setVisible(true);
            paymentCompleted = false;
            paymentCompleteButton.setEnabled(true);
            paymentCompleteButton.setText("✅ Complete Payment");
            revalidate();
            repaint();
        });
    }
    
    // ==================== COMPLETE PAYMENT (UPDATED) ====================
    
    private void completePayment() {
        if (paymentCompleted) {
            showStatusMessage("⚠️", "Payment already completed!");
            return;
        }

        // ========== STEP 1: Find activeAuctionItem ==========
        if (activeAuctionItem == null) {
            String labelText = paymentItemLabel.getText();
            String itemName = "";
            
            if (labelText.startsWith("📦 Item: ")) {
                itemName = labelText.substring(8).trim();
            } else if (labelText.startsWith("Item: ")) {
                itemName = labelText.substring(6).trim();
            } else {
                itemName = labelText.trim();
            }
            
            System.out.println("🔍 Searching for item: '" + itemName + "'");
            
            for (int i = 0; i < itemListModel.getSize(); i++) {
                AuctionItem item = itemListModel.get(i);
                if (item.getName().equals(itemName)) {
                    activeAuctionItem = item;
                    System.out.println("✅ Found in itemListModel: " + item.getName());
                    break;
                }
            }
            
            if (activeAuctionItem == null && allItems != null) {
                for (AuctionItem item : allItems) {
                    if (item.getName().equals(itemName)) {
                        activeAuctionItem = item;
                        System.out.println("✅ Found in allItems: " + item.getName());
                        break;
                    }
                }
            }
            
            if (activeAuctionItem == null) {
                try {
                    List<AuctionItem> serverItems = auctionService.getItems();
                    for (AuctionItem item : serverItems) {
                        if (item.getName().equals(itemName)) {
                            activeAuctionItem = item;
                            System.out.println("✅ Found from server: " + item.getName());
                            break;
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            
            if (activeAuctionItem == null) {
                JOptionPane.showMessageDialog(this, 
                    "⚠️ No item to pay for!\n\n" +
                    "Item Name: '" + itemName + "'\n" +
                    "Please contact admin for assistance.", 
                    "Payment Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // ========== STEP 2: Validate Payment Amount ==========
        String amountStr = paymentAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Please enter payment amount!");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "⚠️ Amount must be greater than 0!");
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Please enter a valid number!");
            return;
        }
        
        // ========== STEP 3: Get Payment Method ==========
        String selectedMethod = (String) paymentMethodCombo.getSelectedItem();
        String methodName = selectedMethod.split(" ")[0] + " " + selectedMethod.split(" ")[1];
        
        // ========== STEP 4: Show Confirmation Dialog ==========
        int confirm = JOptionPane.showConfirmDialog(this,
            "✅ Confirm Payment?\n\n" +
            "👤 Winner: " + currentWinner + "\n" +
            "📦 Item: " + activeAuctionItem.getName() + "\n" +
            "💰 Amount: $" + String.format("%.2f", amount) + "\n" +
            "💳 Payment Method: " + selectedMethod + "\n\n" +
            "Click 'Yes' to complete payment.",
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        // ========== STEP 5: Process Payment ==========
        paymentCompleted = true;
        paymentCompleteButton.setText("✅ Payment Completed!");
        paymentCompleteButton.setEnabled(false);
        
        showStatusMessage("💳", "✅ Payment of $" + String.format("%.2f", amount) + " completed via " + methodName + "!");
        
        // ========== STEP 6: Update Server ==========
        try {
            // Get payment record from server
            PaymentRecord payment = auctionService.getPaymentByItemId(activeAuctionItem.getId());
            if (payment != null) {
                String transactionId = "TXN-" + System.currentTimeMillis();
                auctionService.completePaymentByClient(payment.getId(), currentWinner, methodName, transactionId);
                System.out.println("✅ Payment updated on server: ID=" + payment.getId());
            } else {
                System.out.println("⚠️ No payment record found for item ID: " + activeAuctionItem.getId());
            }
            
            // Broadcast to chat
            auctionService.broadcastChatMessage("💳 PAYMENT", 
                "✅ Payment completed for " + activeAuctionItem.getName() + 
                " - Amount: $" + String.format("%.2f", amount) + 
                " via " + methodName + " by " + currentWinner);
            
            auctionService.broadcastChatMessage("🧾 RECEIPT", 
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "  🧾 PAYMENT RECEIPT\n" +
                "  ──────────────────────────────────────\n" +
                "  📅 Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n" +
                "  👤 Winner: " + currentWinner + "\n" +
                "  📦 Item: " + activeAuctionItem.getName() + "\n" +
                "  💰 Amount: $" + String.format("%.2f", amount) + "\n" +
                "  💳 Method: " + methodName + "\n" +
                "  🆔 TXN: " + "TXN-" + System.currentTimeMillis() + "\n" +
                "  ✅ Status: PAID\n" +
                "  ──────────────────────────────────────\n" +
                "  Thank you for your purchase!\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating payment: " + e.getMessage());
            paymentCompleted = false;
            paymentCompleteButton.setText("✅ Complete Payment");
            paymentCompleteButton.setEnabled(true);
            return;
        }
        
        // ========== STEP 7: Show Receipt ==========
        showPaymentReceipt(currentWinner, activeAuctionItem.getName(), amount, selectedMethod);
        
        // ========== STEP 8: Hide Panel after 4 seconds ==========
        Timer hideTimer = new Timer(4000, e -> {
            hidePaymentPanel();
            paymentCompleteButton.setText("✅ Complete Payment");
            paymentCompleteButton.setEnabled(true);
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }
    
    // ==================== PAYMENT RECEIPT ====================
    
    private void showPaymentReceipt(String winnerName, String itemName, double amount, String method) {
        SwingUtilities.invokeLater(() -> {
            JDialog receiptDialog = new JDialog(this, "🧾 PAYMENT RECEIPT", true);
            receiptDialog.setSize(500, 550);
            receiptDialog.setLocationRelativeTo(this);
            receiptDialog.getContentPane().setBackground(BG_DARK);
            receiptDialog.setLayout(new BorderLayout(10, 10));
            
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(0, 230, 118, 30));
            headerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));
            
            JLabel receiptTitle = new JLabel("🧾 PAYMENT RECEIPT");
            receiptTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
            receiptTitle.setForeground(ACCENT_GREEN);
            receiptTitle.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(receiptTitle, BorderLayout.CENTER);
            
            JLabel receiptId = new JLabel("Receipt #: AUCTION-" + System.currentTimeMillis());
            receiptId.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            receiptId.setForeground(TEXT_SECONDARY);
            receiptId.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(receiptId, BorderLayout.SOUTH);
            
            JPanel contentPanel = new JPanel(new GridLayout(7, 2, 10, 12));
            contentPanel.setBackground(BG_DARK);
            contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
            
            contentPanel.add(createLabel("📅 Date:"));
            contentPanel.add(createLabel(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
            
            contentPanel.add(createLabel("👤 Winner:"));
            contentPanel.add(createValueLabel(winnerName));
            
            contentPanel.add(createLabel("📦 Item:"));
            contentPanel.add(createValueLabel(itemName));
            
            contentPanel.add(createLabel("💰 Amount:"));
            JLabel amountValue = new JLabel("$" + String.format("%.2f", amount));
            amountValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
            amountValue.setForeground(ACCENT_GOLD);
            contentPanel.add(amountValue);
            
            contentPanel.add(createLabel("💳 Payment Method:"));
            contentPanel.add(createValueLabel(method));
            
            contentPanel.add(createLabel("✅ Status:"));
            JLabel statusValue = new JLabel("PAID");
            statusValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
            statusValue.setForeground(ACCENT_GREEN);
            contentPanel.add(statusValue);
            
            contentPanel.add(createLabel("🆔 Transaction ID:"));
            contentPanel.add(createValueLabel("TXN-" + System.currentTimeMillis()));
            
            JPanel footerPanel = new JPanel();
            footerPanel.setBackground(BG_DARK);
            footerPanel.setBorder(new EmptyBorder(5, 20, 15, 20));
            
            JLabel footerLabel = new JLabel("✅ Payment completed successfully! Thank you for your purchase.");
            footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            footerLabel.setForeground(TEXT_SECONDARY);
            footerPanel.add(footerLabel);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.setBackground(BG_DARK);
            
            JButton closeButton = new JButton("✅ Close");
            closeButton.setBackground(ACCENT_GOLD);
            closeButton.setForeground(Color.BLACK);
            closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            closeButton.setBorder(new EmptyBorder(10, 40, 10, 40));
            closeButton.setFocusPainted(false);
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeButton.addActionListener(e -> receiptDialog.dispose());
            
            JButton printButton = new JButton("🖨️ Print");
            printButton.setBackground(ACCENT_BLUE);
            printButton.setForeground(Color.WHITE);
            printButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            printButton.setBorder(new EmptyBorder(10, 30, 10, 30));
            printButton.setFocusPainted(false);
            printButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            printButton.addActionListener(e -> {
                JOptionPane.showMessageDialog(receiptDialog, "🖨️ Receipt sent to printer!");
            });
            
            buttonPanel.add(closeButton);
            buttonPanel.add(printButton);
            
            receiptDialog.add(headerPanel, BorderLayout.NORTH);
            receiptDialog.add(contentPanel, BorderLayout.CENTER);
            receiptDialog.add(footerPanel, BorderLayout.SOUTH);
            receiptDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ex) {
                // Ignore
            }
            
            receiptDialog.setAlwaysOnTop(true);
            receiptDialog.setVisible(true);
        });
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }
    
    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
    
    // ==================== SCHEDULE ALERT BOX ====================
    
    private void showScheduleAlert(String itemName, String startTime, String countdown, String price) {
        SwingUtilities.invokeLater(() -> {
            JDialog alertDialog = new JDialog(this, "📅 Auction Scheduled!", true);
            alertDialog.setSize(500, 400);
            alertDialog.setLocationRelativeTo(this);
            alertDialog.getContentPane().setBackground(BG_DARK);
            alertDialog.setLayout(new BorderLayout(10, 10));
            
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(156, 39, 176, 50));
            headerPanel.setBorder(new EmptyBorder(20, 20, 15, 20));
            
            JLabel iconLabel = new JLabel("🔔");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 50));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(iconLabel, BorderLayout.NORTH);
            
            JLabel titleLabel = new JLabel("📅 NEW AUCTION SCHEDULED!");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            titleLabel.setForeground(ACCENT_GOLD);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            
            JPanel contentPanel = new JPanel(new GridLayout(0, 1, 10, 12));
            contentPanel.setBackground(BG_DARK);
            contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
            
            JLabel itemLabel = new JLabel("📦 Item: " + itemName);
            itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            itemLabel.setForeground(TEXT_PRIMARY);
            contentPanel.add(itemLabel);
            
            JLabel timeLabel = new JLabel("⏰ Starts at: " + startTime);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            timeLabel.setForeground(TEXT_SECONDARY);
            contentPanel.add(timeLabel);
            
            JLabel countdownLabel = new JLabel("⌛ " + countdown + " remaining");
            countdownLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            countdownLabel.setForeground(ACCENT_ORANGE);
            contentPanel.add(countdownLabel);
            
            JLabel priceLabel = new JLabel("💰 Starting Price: $" + price);
            priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            priceLabel.setForeground(ACCENT_GREEN);
            contentPanel.add(priceLabel);
            
            JLabel statusLabel = new JLabel(scheduledModeEnabled ? 
                "✅ Scheduled Mode: ENABLED - Will start automatically!" : 
                "⏸ Scheduled Mode: DISABLED - Please wait for admin to enable!");
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            statusLabel.setForeground(scheduledModeEnabled ? ACCENT_GREEN : ACCENT_ORANGE);
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(statusLabel);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.setBackground(BG_DARK);
            
            JButton okButton = new JButton("✅ Got it!");
            okButton.setBackground(ACCENT_GOLD);
            okButton.setForeground(Color.BLACK);
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            okButton.setBorder(new EmptyBorder(10, 40, 10, 40));
            okButton.setFocusPainted(false);
            okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            okButton.addActionListener(e -> alertDialog.dispose());
            
            buttonPanel.add(okButton);
            
            alertDialog.add(headerPanel, BorderLayout.NORTH);
            alertDialog.add(contentPanel, BorderLayout.CENTER);
            alertDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ex) {
                // Ignore
            }
            
            alertDialog.setAlwaysOnTop(true);
            alertDialog.setVisible(true);
        });
    }
    
    // ==================== WINNER ALERT (UPDATED - WITH IMAGE) ====================
    
    private void showWinnerAlert(String winnerName, String itemName, double amount) {
        SwingUtilities.invokeLater(() -> {
            JDialog alertDialog = new JDialog(this, "🏆 AUCTION WINNER!", true);
            alertDialog.setSize(550, 600);
            alertDialog.setLocationRelativeTo(this);
            alertDialog.getContentPane().setBackground(BG_DARK);
            
            // ============ FIX: Rounded Border for Dialog ============
            // Make dialog undecorated and add custom rounded border
            alertDialog.setUndecorated(true);
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw rounded rectangle background
                    g2d.setColor(BG_DARK);
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    
                    // Draw border
                    g2d.setColor(ACCENT_GOLD);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);
                    
                    g2d.dispose();
                }
            };
            mainPanel.setOpaque(false);
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            mainPanel.setLayout(new BorderLayout(10, 10));
            
            // Close button (X) at top right
            JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            closePanel.setOpaque(false);
            
            JButton closeBtn = new JButton("✕");
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setBackground(new Color(200, 50, 50));
            closeBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> alertDialog.dispose());
            closeBtn.setContentAreaFilled(false);
            closeBtn.setOpaque(true);
            closeBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 50, 50), 1));
            
            closePanel.add(closeBtn);
            mainPanel.add(closePanel, BorderLayout.NORTH);
            
            // ========== HEADER PANEL ==========
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(255, 193, 7, 50));
            headerPanel.setBorder(new EmptyBorder(20, 20, 15, 20));
            
            JLabel iconLabel = new JLabel("🏆");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 60));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(iconLabel, BorderLayout.NORTH);
            
            JLabel titleLabel = new JLabel("🎉 YOU WON THE AUCTION!");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
            titleLabel.setForeground(ACCENT_GOLD);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            
            // ========== CONTENT PANEL ==========
            JPanel contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBackground(BG_DARK);
            contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            
            // ========== ITEM IMAGE ==========
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(130, 130));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 3),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            imageLabel.setBackground(new Color(30, 38, 55));
            imageLabel.setOpaque(true);
            
            // Load item image
            if (activeAuctionItem != null) {
                ImageIcon icon = ImageUtils.getItemImage(activeAuctionItem, 130, 130);
                if (icon != null && icon.getImage() != null) {
                    imageLabel.setIcon(icon);
                    imageLabel.setText("");
                } else {
                    imageLabel.setText("🖼️");
                    imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                    imageLabel.setForeground(TEXT_SECONDARY);
                }
            } else {
                AuctionItem foundItem = null;
                for (int i = 0; i < itemListModel.getSize(); i++) {
                    AuctionItem item = itemListModel.get(i);
                    if (item.getName().equals(itemName)) {
                        foundItem = item;
                        break;
                    }
                }
                if (foundItem == null && allItems != null) {
                    for (AuctionItem item : allItems) {
                        if (item.getName().equals(itemName)) {
                            foundItem = item;
                            break;
                        }
                    }
                }
                if (foundItem != null) {
                    ImageIcon icon = ImageUtils.getItemImage(foundItem, 130, 130);
                    if (icon != null && icon.getImage() != null) {
                        imageLabel.setIcon(icon);
                        imageLabel.setText("");
                    } else {
                        imageLabel.setText("🖼️");
                        imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                        imageLabel.setForeground(TEXT_SECONDARY);
                    }
                } else {
                    imageLabel.setText("🖼️");
                    imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                    imageLabel.setForeground(TEXT_SECONDARY);
                }
            }
            
            contentPanel.add(imageLabel, gbc);
            
            // ========== WINNER INFO ==========
            gbc.gridy = 1;
            
            JLabel winnerLabel2 = new JLabel("👤 Winner: " + winnerName + " (YOU)");
            winnerLabel2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            winnerLabel2.setForeground(ACCENT_GREEN);
            winnerLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(winnerLabel2, gbc);
            
            gbc.gridy = 2;
            JLabel itemLabel2 = new JLabel("📦 Item: " + itemName);
            itemLabel2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            itemLabel2.setForeground(TEXT_PRIMARY);
            itemLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(itemLabel2, gbc);
            
            gbc.gridy = 3;
            JLabel amountLabel2 = new JLabel("💰 Final Price: $" + String.format("%.2f", amount));
            amountLabel2.setFont(new Font("Segoe UI", Font.BOLD, 24));
            amountLabel2.setForeground(ACCENT_GOLD);
            amountLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(amountLabel2, gbc);
            
            gbc.gridy = 4;
            JLabel paymentAlert = new JLabel("💳 Please proceed to payment!");
            paymentAlert.setFont(new Font("Segoe UI", Font.BOLD, 18));
            paymentAlert.setForeground(ACCENT_BLUE);
            paymentAlert.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(paymentAlert, gbc);
            
            // ========== BUTTON PANEL ==========
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.setBackground(BG_DARK);
            
            JButton okButton = new JButton("✅ Go to Payment");
            okButton.setBackground(ACCENT_GREEN);
            okButton.setForeground(Color.BLACK);
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            okButton.setBorder(new EmptyBorder(12, 50, 12, 50));
            okButton.setFocusPainted(false);
            okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            okButton.addActionListener(e -> {
                alertDialog.dispose();
            });
            
            buttonPanel.add(okButton);
            
            // Add components to main panel
            mainPanel.add(headerPanel, BorderLayout.NORTH);
            mainPanel.add(contentPanel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            
            alertDialog.add(mainPanel);
            
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ex) {
                // Ignore
            }
            
            alertDialog.setAlwaysOnTop(true);
            alertDialog.setVisible(true);
        });
    }
    
    // ==================== OTHER WINNER ALERT (UPDATED - WITH IMAGE) ====================
    
    private void showOtherWinnerAlert(String winnerName, String itemName, double amount) {
        SwingUtilities.invokeLater(() -> {
            JDialog alertDialog = new JDialog(this, "🏆 Auction Result", true);
            alertDialog.setSize(480, 420);
            alertDialog.setLocationRelativeTo(this);
            alertDialog.getContentPane().setBackground(BG_DARK);
            alertDialog.setLayout(new BorderLayout(10, 10));
            
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(255, 193, 7, 30));
            headerPanel.setBorder(new EmptyBorder(15, 20, 10, 20));
            
            JLabel iconLabel = new JLabel("🏆");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 50));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(iconLabel, BorderLayout.NORTH);
            
            JLabel titleLabel = new JLabel("AUCTION ENDED!");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            titleLabel.setForeground(ACCENT_GOLD);
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            
            JPanel contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBackground(BG_DARK);
            contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            
            // ========== ITEM IMAGE ==========
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            
            JLabel imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(100, 100));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);
            imageLabel.setBorder(BorderFactory.createLineBorder(ACCENT_GOLD, 2));
            imageLabel.setBackground(new Color(30, 38, 55));
            imageLabel.setOpaque(true);
            
            // Try to find item image
            AuctionItem foundItem = null;
            for (int i = 0; i < itemListModel.getSize(); i++) {
                AuctionItem item = itemListModel.get(i);
                if (item.getName().equals(itemName)) {
                    foundItem = item;
                    break;
                }
            }
            if (foundItem == null && allItems != null) {
                for (AuctionItem item : allItems) {
                    if (item.getName().equals(itemName)) {
                        foundItem = item;
                        break;
                    }
                }
            }
            if (foundItem != null) {
                ImageIcon icon = ImageUtils.getItemImage(foundItem, 100, 100);
                if (icon != null && icon.getImage() != null) {
                    imageLabel.setIcon(icon);
                    imageLabel.setText("");
                } else {
                    imageLabel.setText("🖼️");
                    imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
                    imageLabel.setForeground(TEXT_SECONDARY);
                }
            } else {
                imageLabel.setText("🖼️");
                imageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
                imageLabel.setForeground(TEXT_SECONDARY);
            }
            
            contentPanel.add(imageLabel, gbc);
            
            // ========== INFO ==========
            gbc.gridy = 1;
            
            JLabel winnerLabel2 = new JLabel("👤 Winner: " + winnerName);
            winnerLabel2.setFont(new Font("Segoe UI", Font.BOLD, 16));
            winnerLabel2.setForeground(ACCENT_GREEN);
            winnerLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(winnerLabel2, gbc);
            
            gbc.gridy = 2;
            JLabel itemLabel2 = new JLabel("📦 Item: " + itemName);
            itemLabel2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            itemLabel2.setForeground(TEXT_SECONDARY);
            itemLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(itemLabel2, gbc);
            
            gbc.gridy = 3;
            JLabel amountLabel2 = new JLabel("💰 Final Price: $" + String.format("%.2f", amount));
            amountLabel2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            amountLabel2.setForeground(ACCENT_GOLD);
            amountLabel2.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(amountLabel2, gbc);
            
            gbc.gridy = 4;
            JLabel notYouLabel = new JLabel("😊 Better luck next time!");
            notYouLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            notYouLabel.setForeground(ACCENT_BLUE);
            notYouLabel.setHorizontalAlignment(SwingConstants.CENTER);
            contentPanel.add(notYouLabel, gbc);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.setBackground(BG_DARK);
            
            JButton okButton = new JButton("✅ Got it!");
            okButton.setBackground(ACCENT_GOLD);
            okButton.setForeground(Color.BLACK);
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            okButton.setBorder(new EmptyBorder(10, 40, 10, 40));
            okButton.setFocusPainted(false);
            okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            okButton.addActionListener(e -> alertDialog.dispose());
            
            buttonPanel.add(okButton);
            
            alertDialog.add(headerPanel, BorderLayout.NORTH);
            alertDialog.add(contentPanel, BorderLayout.CENTER);
            alertDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception ex) {
                // Ignore
            }
            
            alertDialog.setAlwaysOnTop(true);
            alertDialog.setVisible(true);
        });
    }
    
    // ==================== STATUS MESSAGE ====================
    
    private void showStatusMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(sender + " " + message);
            statusLabel.setForeground(ACCENT_GOLD);
            Timer timer = new Timer(3000, e -> {
                statusLabel.setText("✅ Connected");
                statusLabel.setForeground(ACCENT_GREEN);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    
    // ==================== TIMERS ====================
    
    private void startTimers() {
        refreshTimer = new Timer(5000, e -> {
            refreshItemList();
            try {
                if (auctionService != null) {
                    scheduledModeEnabled = auctionService.isScheduledModeEnabled();
                    updateScheduleStatus();
                }
            } catch (RemoteException ex) {
                // ignore
            }
        });
        refreshTimer.start();
        
        countdownTimer = new Timer(1000, e -> updateCountdown());
        countdownTimer.start();
    }
    
    // ==================== updateCountdown() ====================
    
    private void updateCountdown() {
        if (activeAuctionItem != null && activeAuctionItem.isActive() && !activeAuctionItem.isSold()) {
            isAuctionRunning = true;
            auctionStarted = true;
            long remaining = activeAuctionItem.getRemainingSeconds();
            
            timeLeftTitleLabel.setText("⏱️ Time Left: " + activeAuctionItem.getFormattedRemainingTime());
            timeLeftTitleLabel.setForeground(remaining < 30 ? ACCENT_RED : ACCENT_GOLD);
            
            if (remaining > 0) {
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                timeLeftLabel.setText(String.format("%02d:%02d", minutes, seconds));
                timeLeftLabel.setForeground(remaining < 30 ? ACCENT_RED : ACCENT_GOLD);
                
                startTimeLabel.setText("⏰ Started now!");
                startTimeLabel.setForeground(ACCENT_GREEN);
                
                bidButton.setEnabled(true);
                bidAmountDisplayLabel.setText("$" + (int)activeAuctionItem.getCurrentPrice());
                
                nextAuctionLabel.setText("");
                scheduleStatusLabel.setText("🔥 AUCTION IN PROGRESS!");
                scheduleStatusLabel.setForeground(ACCENT_RED);
                scheduleStatusLabel.setBackground(new Color(ACCENT_RED.getRed(), ACCENT_RED.getGreen(), ACCENT_RED.getBlue(), 30));
            } else {
                timeLeftLabel.setText("00:00");
                timeLeftLabel.setForeground(ACCENT_RED);
                timeLeftTitleLabel.setForeground(ACCENT_RED);
                bidButton.setEnabled(false);
            }
            return;
        }
        
        if (hasSchedule && !auctionStarted) {
            try {
                if (auctionService != null) {
                    scheduledModeEnabled = auctionService.isScheduledModeEnabled();
                }
            } catch (RemoteException e) {
                // ignore
            }
            
            if (scheduledModeEnabled) {
                long remaining = (scheduledRemainingSeconds > 0) ? scheduledRemainingSeconds : 0;
                if (remaining > 0) {
                    timeLeftLabel.setText("⏳ " + formatCountdown(remaining));
                    timeLeftLabel.setForeground(ACCENT_ORANGE);
                    timeLeftTitleLabel.setText("⏱️ Starts in: " + formatCountdown(remaining));
                    timeLeftTitleLabel.setForeground(ACCENT_ORANGE);
                } else {
                    timeLeftLabel.setText("🔔 Starting soon!");
                    timeLeftLabel.setForeground(ACCENT_GREEN);
                    timeLeftTitleLabel.setText("⏱️ Starting soon!");
                    timeLeftTitleLabel.setForeground(ACCENT_GREEN);
                }
                
                startTimeLabel.setText("⏰ Starts at: " + scheduledStartTime);
                startTimeLabel.setForeground(ACCENT_GOLD);
                
                scheduleStatusLabel.setText("✅ Scheduled Mode: ENABLED - Waiting for " + scheduledStartTime);
                scheduleStatusLabel.setForeground(ACCENT_GREEN);
                scheduleStatusLabel.setBackground(new Color(ACCENT_GREEN.getRed(), ACCENT_GREEN.getGreen(), ACCENT_GREEN.getBlue(), 30));
            } else {
                timeLeftLabel.setText("⏸ Waiting");
                timeLeftLabel.setForeground(TEXT_SECONDARY);
                timeLeftTitleLabel.setText("⏱️ Waiting for activation");
                timeLeftTitleLabel.setForeground(TEXT_SECONDARY);
                startTimeLabel.setText("");
                scheduleStatusLabel.setText("⏸ Scheduled Mode: DISABLED - Please enable to start");
                scheduleStatusLabel.setForeground(ACCENT_ORANGE);
                scheduleStatusLabel.setBackground(new Color(ACCENT_ORANGE.getRed(), ACCENT_ORANGE.getGreen(), ACCENT_ORANGE.getBlue(), 30));
            }
            
            bidButton.setEnabled(false);
            
            nextAuctionLabel.setText("📅 Next Auction: " + scheduledItemName + " ($" + scheduledPrice + ")");
            nextAuctionLabel.setForeground(ACCENT_GOLD);
            
            currentItemNameLabel.setText("⏳ " + scheduledItemName);
            currentItemDescLabel.setText("Starts at: " + scheduledStartTime);
            currentPriceLabel.setText("$" + scheduledPrice);
            currentWinnerLabel.setText("Not started yet");
            
            currentItemImageLabel.setText("⏳");
            currentItemImageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
            currentItemImageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            currentItemImageLabel.setVerticalTextPosition(SwingConstants.CENTER);
            return;
        }
        
        timeLeftLabel.setText("--:--");
        timeLeftLabel.setForeground(TEXT_SECONDARY);
        timeLeftTitleLabel.setText("⏱️ Waiting for auction");
        timeLeftTitleLabel.setForeground(TEXT_SECONDARY);
        startTimeLabel.setText("");
        bidButton.setEnabled(false);
        nextAuctionLabel.setText("");
        
        if (!hasSchedule) {
            scheduleStatusLabel.setText("📅 No schedule available - Please wait for admin");
            scheduleStatusLabel.setForeground(TEXT_SECONDARY);
            scheduleStatusLabel.setBackground(new Color(100, 100, 100, 30));
            
            currentItemNameLabel.setText("⏳ Waiting for auction...");
            currentItemDescLabel.setText("No auction scheduled yet");
            currentPriceLabel.setText("$0.00");
            currentWinnerLabel.setText("No bids");
            
            currentItemImageLabel.setText("🖼️");
            currentItemImageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
            currentItemImageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            currentItemImageLabel.setVerticalTextPosition(SwingConstants.CENTER);
        }
    }
    
    private String formatCountdown(long seconds) {
        if (seconds <= 0) return "0s";
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
    
    // ==================== CLIENT CALLBACK ====================
    
    private class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {
        
        protected ClientCallbackImpl() throws RemoteException {
            super();
        }
        
        @Override
        public void notifyBidUpdate(int itemId, double newPrice, String bidderName) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                if (activeAuctionItem != null && activeAuctionItem.getId() == itemId) {
                    currentPriceLabel.setText("$" + String.format("%.2f", newPrice));
                    currentWinnerLabel.setText(bidderName.isEmpty() ? "No bids" : bidderName);
                    bidAmountDisplayLabel.setText("$" + (int)newPrice);
                    activeAuctionItem.setCurrentPrice(newPrice);
                    activeAuctionItem.setCurrentWinner(bidderName);
                }
                refreshItemList();
            });
        }
        
        @Override
        public void notifyAuctionEnd(int itemId, String winner, double finalPrice) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                if (activeAuctionItem != null && activeAuctionItem.getId() == itemId) {
                    if (winner.equals(clientName)) {
                        showWinnerAlert(winner, activeAuctionItem.getName(), finalPrice);
                        currentWinner = winner;
                        showPaymentPanel(winner, activeAuctionItem.getName(), finalPrice);
                        showStatusMessage("🏆", "🎉 YOU WON! " + activeAuctionItem.getName() + " for $" + String.format("%.2f", finalPrice));
                    } else {
                        showStatusMessage("🏆", winner + " won " + activeAuctionItem.getName() + " for $" + String.format("%.2f", finalPrice));
                        showOtherWinnerAlert(winner, activeAuctionItem.getName(), finalPrice);
                    }
                    
                    activeAuctionItem = null;
                    isAuctionRunning = false;
                    auctionStarted = false;
                    
                    currentItemNameLabel.setText("✅ Auction Ended");
                    currentItemDescLabel.setText("");
                    currentPriceLabel.setText("$" + String.format("%.2f", finalPrice));
                    currentWinnerLabel.setText(winner);
                    timeLeftLabel.setText("00:00");
                    timeLeftLabel.setForeground(ACCENT_RED);
                    timeLeftTitleLabel.setText("⏱️ Auction Ended");
                    timeLeftTitleLabel.setForeground(ACCENT_RED);
                    startTimeLabel.setText("");
                    bidButton.setEnabled(false);
                    nextAuctionLabel.setText("");
                    
                    currentItemImageLabel.setText("✅ Sold");
                    currentItemImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    currentItemImageLabel.setForeground(ACCENT_GOLD);
                    
                    scheduleStatusLabel.setText("✅ Auction Ended - Winner: " + winner);
                    scheduleStatusLabel.setForeground(ACCENT_GOLD);
                    scheduleStatusLabel.setBackground(new Color(ACCENT_GOLD.getRed(), ACCENT_GOLD.getGreen(), ACCENT_GOLD.getBlue(), 30));
                    
                    refreshItemList();
                    updateChatCount();
                }
            });
        }
        
        @Override
        public void receiveChatMessage(String sender, String message) throws RemoteException {
            addChatMessage(sender, message);
            
            if (sender.equals("💬 SYSTEM")) {
                SwingUtilities.invokeLater(() -> updateChatCount());
            }
        }
        
        @Override
        public String getClientName() throws RemoteException {
            return clientName;
        }
        
        @Override
        public void notifyNewItem(AuctionItem item) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                refreshItemList();
                showStatusMessage("📦", "New item added: " + item.getName());
            });
        }
        
        @Override
        public void notifyNewAuctionStart(AuctionItem item) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                activeAuctionItem = item;
                isAuctionRunning = true;
                auctionStarted = true;
                hasSchedule = false;
                paymentCompleted = false;
                hidePaymentPanel();
                
                currentItemNameLabel.setText(item.getName());
                
                String detailedInfo = item.getDetailedInfo();
                if (detailedInfo != null && !detailedInfo.isEmpty()) {
                    currentItemDescLabel.setText("<html><body style='width: 300px;'>" + 
                        detailedInfo.replace("\n", "<br>") + "</body></html>");
                } else {
                    currentItemDescLabel.setText(item.getDescription());
                }
                
                currentPriceLabel.setText("$" + String.format("%.2f", item.getCurrentPrice()));
                currentWinnerLabel.setText("No bids");
                nextAuctionLabel.setText("");
                
                ImageIcon icon = ImageUtils.getItemImage(item, 250, 250);
                if (icon != null && icon.getImage() != null) {
                    currentItemImageLabel.setIcon(icon);
                    currentItemImageLabel.setText("");
                } else {
                    currentItemImageLabel.setText("🖼️ No Image");
                    currentItemImageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    currentItemImageLabel.setForeground(TEXT_SECONDARY);
                }
                
                timeLeftLabel.setText(item.getFormattedRemainingTime());
                timeLeftLabel.setForeground(ACCENT_GOLD);
                timeLeftTitleLabel.setText("⏱️ " + item.getFormattedRemainingTime());
                timeLeftTitleLabel.setForeground(ACCENT_GOLD);
                startTimeLabel.setText("⏰ Started now!");
                startTimeLabel.setForeground(ACCENT_GREEN);
                
                bidButton.setEnabled(true);
                bidAmountDisplayLabel.setText("$" + (int)item.getCurrentPrice());
                
                scheduleStatusLabel.setText("🔥 AUCTION STARTED: " + item.getName());
                scheduleStatusLabel.setForeground(ACCENT_RED);
                scheduleStatusLabel.setBackground(new Color(ACCENT_RED.getRed(), ACCENT_RED.getGreen(), ACCENT_RED.getBlue(), 30));
                
                refreshItemList();
                showStatusMessage("🎯", "🔥 Auction started: " + item.getName());
            });
        }
        
        @Override
        public void notifyItemListRefresh() throws RemoteException {
            SwingUtilities.invokeLater(() -> refreshItemList());
        }
        
        @Override
        public void receiveScheduleAlert(String itemName, String startTime, String countdown, String price, long remainingSeconds) throws RemoteException {
            SwingUtilities.invokeLater(() -> {
                scheduledItemName = itemName;
                scheduledStartTime = startTime;
                scheduledPrice = price;
                scheduledRemainingSeconds = remainingSeconds;
                hasSchedule = true;
                auctionStarted = false;
                
                try {
                    if (auctionService != null) {
                        scheduledModeEnabled = auctionService.isScheduledModeEnabled();
                    }
                } catch (RemoteException e) {
                    // ignore
                }
                
                showScheduleAlert(itemName, startTime, countdown, price);
                
                if (scheduledModeEnabled) {
                    timeLeftLabel.setText("⏳ " + countdown);
                    timeLeftLabel.setForeground(ACCENT_ORANGE);
                    timeLeftTitleLabel.setText("⏱️ Starts in: " + countdown);
                    timeLeftTitleLabel.setForeground(ACCENT_ORANGE);
                    startTimeLabel.setText("⏰ Starts at: " + startTime);
                    startTimeLabel.setForeground(ACCENT_GOLD);
                    scheduleStatusLabel.setText("✅ Scheduled Mode: ENABLED - Starts at " + startTime);
                    scheduleStatusLabel.setForeground(ACCENT_GREEN);
                    scheduleStatusLabel.setBackground(new Color(ACCENT_GREEN.getRed(), ACCENT_GREEN.getGreen(), ACCENT_GREEN.getBlue(), 30));
                } else {
                    timeLeftLabel.setText("⏸ Waiting");
                    timeLeftLabel.setForeground(TEXT_SECONDARY);
                    timeLeftTitleLabel.setText("⏱️ Waiting for activation");
                    timeLeftTitleLabel.setForeground(TEXT_SECONDARY);
                    startTimeLabel.setText("");
                    scheduleStatusLabel.setText("⏸ Scheduled Mode: DISABLED - Waiting for admin to enable...");
                    scheduleStatusLabel.setForeground(ACCENT_ORANGE);
                    scheduleStatusLabel.setBackground(new Color(ACCENT_ORANGE.getRed(), ACCENT_ORANGE.getGreen(), ACCENT_ORANGE.getBlue(), 30));
                }
                
                bidButton.setEnabled(false);
                
                currentItemNameLabel.setText("⏳ " + itemName);
                currentItemDescLabel.setText("Starts at: " + startTime);
                currentPriceLabel.setText("$" + price);
                currentWinnerLabel.setText("Not started yet");
                nextAuctionLabel.setText("📅 Next Auction: " + itemName + " ($" + price + ")");
                nextAuctionLabel.setForeground(ACCENT_GOLD);
                
                currentItemImageLabel.setText("⏳");
                currentItemImageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                currentItemImageLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                currentItemImageLabel.setVerticalTextPosition(SwingConstants.CENTER);
                
                showStatusMessage("📅", "Next auction: " + itemName + " at " + startTime);
            });
        }

        @Override
        public void notifyPaymentUpdate(PaymentRecord payment) throws RemoteException {
            // Handle payment update if needed
        }
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            AuctionClientGUI client = new AuctionClientGUI();
            client.setVisible(true);
        });
    }
}