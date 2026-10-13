package server;

import server.PaymentRecord;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AuctionServerGUI extends JFrame {
    
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JLabel activeAuctionsLabel;
    private JLabel clientCountLabel;
    private JLabel currentItemLabel;
    private JLabel scheduledModeLabel;
    private JTable clientTable;
    private DefaultTableModel clientTableModel;
    private AuctionServiceImpl auctionService;
    private Timer statsTimer;
    private Timer clientListTimer;
    private JButton addItemButton;
    private JButton createScheduleButton;
    private JButton toggleScheduledModeButton;
    private boolean serverStarted = false;
    private boolean scheduledModeEnabled = false;
    
    // Payment Tab Components
    private JTabbedPane tabbedPane;
    private JTable paymentTable;
    private DefaultTableModel paymentTableModel;
    private JTextField searchWinnerField;
    private JComboBox<String> statusFilterCombo;
    private JLabel paymentTotalLabel;
    private JLabel paymentPendingLabel;
    private JLabel paymentCompletedLabel;
    private JLabel paymentFailedLabel;
    
    private static final Color BG_DARK = new Color(18, 25, 35);
    private static final Color BG_CARD = new Color(28, 35, 48);
    private static final Color BG_HOVER = new Color(38, 45, 58);
    private static final Color ACCENT_GOLD = new Color(255, 193, 7);
    private static final Color ACCENT_BLUE = new Color(0, 123, 255);
    private static final Color ACCENT_GREEN = new Color(40, 167, 69);
    private static final Color ACCENT_RED = new Color(220, 53, 69);
    private static final Color ACCENT_PURPLE = new Color(156, 39, 176);
    private static final Color ACCENT_ORANGE = new Color(255, 152, 0);
    private static final Color ACCENT_CYAN = new Color(23, 162, 184);
    private static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(170, 180, 195);
    
    public static final int RMI_PORT = 1099;
    
    public AuctionServerGUI() {
        setTitle("🏛 PREMIUM AUCTION HOUSE - SERVER ADMIN PANEL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setBackground(BG_DARK);
        
        setupUI();
        startServer();
        startTimers();
    }
    
    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(BG_DARK);
        
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // ============ CREATE TABBED PANE ============
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tabbedPane.setForeground(TEXT_PRIMARY);
        
        // Tab 1: Dashboard
        JPanel dashboardPanel = createDashboardPanel();
        tabbedPane.addTab("  📊  DASHBOARD  ", dashboardPanel);
        
        // Tab 2: Payment Management
        JPanel paymentPanel = createPaymentManagementPanel();
        tabbedPane.addTab("  💳  PAYMENTS  ", paymentPanel);
        
        // ============ FIX: Set background colors AFTER adding tabs ============
        // Set tab background colors
        tabbedPane.setBackgroundAt(0, new Color(40, 50, 70));
        tabbedPane.setBackgroundAt(1, new Color(40, 50, 70));
        
        // Set tab foreground colors
        tabbedPane.setForegroundAt(0, ACCENT_GOLD);
        tabbedPane.setForegroundAt(1, ACCENT_GOLD);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_DARK);
        
        // Dashboard Header
        JPanel dashHeader = new JPanel(new BorderLayout());
        dashHeader.setBackground(BG_CARD);
        dashHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_GOLD, 1),
            new EmptyBorder(10, 20, 10, 20)
        ));
        
        JLabel dashTitle = new JLabel("📊 SERVER DASHBOARD");
        dashTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dashTitle.setForeground(ACCENT_GOLD);
        dashHeader.add(dashTitle, BorderLayout.WEST);
        
        JLabel dashSub = new JLabel("Real-time server status and control");
        dashSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dashSub.setForeground(TEXT_SECONDARY);
        dashHeader.add(dashSub, BorderLayout.EAST);
        
        panel.add(dashHeader, BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.45);
        splitPane.setBorder(null);
        splitPane.setBackground(BG_DARK);
        
        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();
        
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(25, 35, 50), 
                                                      getWidth(), 0, new Color(35, 45, 65));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setBorder(new EmptyBorder(20, 30, 20, 30));
        header.setOpaque(false);
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("🏛 PREMIUM AUCTION HOUSE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_PRIMARY);
        
        JLabel subtitleLabel = new JLabel("SERVER ADMINISTRATION DASHBOARD");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(ACCENT_GOLD);
        
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        statusPanel.setOpaque(false);
        
        statusLabel = new JLabel("● STARTING") {
            private Timer blinkTimer;
            private boolean visible = true;
            {
                blinkTimer = new Timer(500, e -> {
                    visible = !visible;
                    setForeground(visible ? new Color(255, 200, 100) : new Color(255, 200, 100, 80));
                });
                blinkTimer.start();
            }
        };
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(255, 200, 100));
        
        JPanel statsBadge = createStatsBadge();
        
        statusPanel.add(statsBadge);
        statusPanel.add(Box.createHorizontalStrut(15));
        statusPanel.add(statusLabel);
        
        header.add(titlePanel, BorderLayout.WEST);
        header.add(statusPanel, BorderLayout.EAST);
        
        return header;
    }
    
    private JPanel createStatsBadge() {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        badge.setBackground(new Color(0, 0, 0, 50));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 30), 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        badge.setOpaque(false);
        
        JLabel uptimeLabel = new JLabel("🟢 SYSTEM ONLINE");
        uptimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        uptimeLabel.setForeground(ACCENT_GREEN);
        badge.add(uptimeLabel);
        
        return badge;
    }
    
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(15, 15));
        leftPanel.setBackground(BG_DARK);
        
        JPanel statsPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        statsPanel.setBackground(BG_DARK);
        statsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JPanel ipCard = createGradientStatCard("🌐 SERVER IP", "Detecting...", ACCENT_BLUE);
        ipLabel = (JLabel) ((JPanel) ipCard.getComponent(1)).getComponent(0);
        
        JPanel portCard = createGradientStatCard("🔌 RMI PORT", String.valueOf(RMI_PORT), ACCENT_BLUE);
        
        JPanel auctionsCard = createGradientStatCard("🔥 ACTIVE AUCTIONS", "0", ACCENT_GOLD);
        activeAuctionsLabel = (JLabel) ((JPanel) auctionsCard.getComponent(1)).getComponent(0);
        
        JPanel clientsCard = createGradientStatCard("👥 CONNECTED CLIENTS", "0", ACCENT_GREEN);
        clientCountLabel = (JLabel) ((JPanel) clientsCard.getComponent(1)).getComponent(0);
        
        JPanel currentItemCard = createGradientStatCard("🎯 CURRENT ITEM", "Waiting...", ACCENT_GOLD);
        currentItemLabel = (JLabel) ((JPanel) currentItemCard.getComponent(1)).getComponent(0);
        currentItemLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        JPanel modeCard = createGradientStatCard("📋 SCHEDULED MODE", "DISABLED", ACCENT_RED);
        scheduledModeLabel = (JLabel) ((JPanel) modeCard.getComponent(1)).getComponent(0);
        
        statsPanel.add(ipCard);
        statsPanel.add(portCard);
        statsPanel.add(auctionsCard);
        statsPanel.add(clientsCard);
        statsPanel.add(currentItemCard);
        statsPanel.add(modeCard);
        
        leftPanel.add(statsPanel, BorderLayout.NORTH);
        
        JPanel controlPanel = createControlPanel();
        leftPanel.add(controlPanel, BorderLayout.CENTER);
        
        return leftPanel;
    }
    
    private JPanel createGradientStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_CARD, getWidth(), getHeight(), BG_HOVER);
                g2d.setPaint(gp);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(TEXT_SECONDARY);
        
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        valuePanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        
        valuePanel.add(valueLabel);
        
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valuePanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(BG_DARK);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel panelTitle = new JLabel("🎮 SERVER CONTROLS");
        panelTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panelTitle.setForeground(ACCENT_GOLD);
        controlPanel.add(panelTitle, BorderLayout.NORTH);
        
        JPanel buttonsGrid = new JPanel(new GridLayout(2, 2, 15, 15));
        buttonsGrid.setOpaque(false);
        buttonsGrid.setBorder(new EmptyBorder(15, 5, 5, 5));
        
        addItemButton = createStyledButton("➕ ADD NEW ITEM", new Color(0, 123, 255));
        createScheduleButton = createStyledButton("📅 CREATE SCHEDULE", new Color(111, 66, 193));
        toggleScheduledModeButton = createStyledButton("🔘 ENABLE SCHEDULED MODE", new Color(220, 53, 69));
        JButton refreshButton = createStyledButton("🔄 REFRESH", new Color(23, 162, 184));
        
        refreshButton.addActionListener(e -> refreshAll());
        
        addItemButton.setEnabled(false);
        createScheduleButton.setEnabled(false);
        toggleScheduledModeButton.setEnabled(false);
        
        addItemButton.addActionListener(e -> showAddItemDialog());
        createScheduleButton.addActionListener(e -> showCreateScheduleDialog());
        toggleScheduledModeButton.addActionListener(e -> toggleScheduledMode());
        
        buttonsGrid.add(addItemButton);
        buttonsGrid.add(createScheduleButton);
        buttonsGrid.add(toggleScheduledModeButton);
        buttonsGrid.add(refreshButton);
        
        controlPanel.add(buttonsGrid, BorderLayout.CENTER);
        
        return controlPanel;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(bgColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(bgColor.brighter());
                } else {
                    g2d.setColor(bgColor);
                }
                
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(15, 15));
        rightPanel.setBackground(BG_DARK);
        
        JPanel clientPanel = new JPanel(new BorderLayout(10, 10));
        clientPanel.setBackground(BG_CARD);
        clientPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel clientTitle = new JLabel("👥 CONNECTED CLIENTS");
        clientTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clientTitle.setForeground(ACCENT_GOLD);
        clientPanel.add(clientTitle, BorderLayout.NORTH);
        
        String[] columns = {"#", "Client Name", "Status", "Connected Since"};
        clientTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        clientTable = new JTable(clientTableModel);
        clientTable.setBackground(new Color(35, 42, 55));
        clientTable.setForeground(TEXT_SECONDARY);
        clientTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clientTable.setRowHeight(32);
        clientTable.setGridColor(new Color(45, 52, 65));
        clientTable.setSelectionBackground(ACCENT_BLUE);
        clientTable.setSelectionForeground(Color.WHITE);
        clientTable.getTableHeader().setBackground(new Color(30, 37, 48));
        clientTable.getTableHeader().setForeground(ACCENT_GOLD);
        clientTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        clientTable.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(45, 52, 65)));
        
        JScrollPane scrollPane = new JScrollPane(clientTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(35, 42, 55));
        
        clientPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel logPanel = new JPanel(new BorderLayout(10, 10));
        logPanel.setBackground(BG_CARD);
        logPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel logTitle = new JLabel("📋 SERVER LOG");
        logTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logTitle.setForeground(ACCENT_GOLD);
        logPanel.add(logTitle, BorderLayout.NORTH);
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(22, 28, 38));
        logArea.setForeground(new Color(180, 190, 205));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.getViewport().setBackground(new Color(22, 28, 38));
        
        logPanel.add(logScroll, BorderLayout.CENTER);
        logPanel.setPreferredSize(new Dimension(0, 250));
        
        rightPanel.add(clientPanel, BorderLayout.CENTER);
        rightPanel.add(logPanel, BorderLayout.SOUTH);
        
        return rightPanel;
    }
    
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        bottomPanel.setBackground(BG_DARK);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(45, 52, 65)));
        
        JLabel tipLabel1 = new JLabel("💡 TIP:");
        tipLabel1.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tipLabel1.setForeground(ACCENT_GOLD);
        
        JLabel tipLabel2 = new JLabel("Use 'Scheduled Mode' to control auction start times");
        tipLabel2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tipLabel2.setForeground(TEXT_SECONDARY);
        
        JLabel tipLabel3 = new JLabel("|");
        tipLabel3.setForeground(new Color(60, 70, 85));
        
        JLabel tipLabel4 = new JLabel("💳 View and manage payments in 'PAYMENTS' tab");
        tipLabel4.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tipLabel4.setForeground(TEXT_SECONDARY);
        
        bottomPanel.add(tipLabel1);
        bottomPanel.add(tipLabel2);
        bottomPanel.add(tipLabel3);
        bottomPanel.add(tipLabel4);
        
        return bottomPanel;
    }
    
    // ============================================================
    // ============ PAYMENT MANAGEMENT PANEL ============
    // ============================================================
    
    private JPanel createPaymentManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Header with Stats
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBackground(BG_DARK);
        
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        titlePanel.setBackground(BG_DARK);
        
        JLabel titleLabel = new JLabel("💳 PAYMENT MANAGEMENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ACCENT_GOLD);
        titlePanel.add(titleLabel);
        
        JLabel subLabel = new JLabel("Manage and track all payment transactions");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(TEXT_SECONDARY);
        titlePanel.add(subLabel);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        // Stats Panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        statsPanel.setBackground(BG_DARK);
        
        // Total
        JPanel totalCard = createSmallStatCard("💰 Total", "$0.00", ACCENT_GREEN);
        paymentTotalLabel = (JLabel) ((JPanel) totalCard.getComponent(1)).getComponent(0);
        statsPanel.add(totalCard);
        
        // Pending
        JPanel pendingCard = createSmallStatCard("⏳ Pending", "0", ACCENT_ORANGE);
        paymentPendingLabel = (JLabel) ((JPanel) pendingCard.getComponent(1)).getComponent(0);
        statsPanel.add(pendingCard);
        
        // Completed
        JPanel completedCard = createSmallStatCard("✅ Completed", "0", ACCENT_GREEN);
        paymentCompletedLabel = (JLabel) ((JPanel) completedCard.getComponent(1)).getComponent(0);
        statsPanel.add(completedCard);
        
        // Failed
        JPanel failedCard = createSmallStatCard("❌ Failed", "0", ACCENT_RED);
        paymentFailedLabel = (JLabel) ((JPanel) failedCard.getComponent(1)).getComponent(0);
        statsPanel.add(failedCard);
        
        headerPanel.add(statsPanel, BorderLayout.EAST);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Search/Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(BG_CARD);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 80), 1),
            new EmptyBorder(10, 15, 10, 15)
        ));
        
        JLabel searchLabel = new JLabel("🔍 Search Winner:");
        searchLabel.setForeground(TEXT_PRIMARY);
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filterPanel.add(searchLabel);
        
        searchWinnerField = new JTextField(15);
        searchWinnerField.setBackground(new Color(45, 52, 65));
        searchWinnerField.setForeground(TEXT_PRIMARY);
        searchWinnerField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchWinnerField.addActionListener(e -> loadPaymentData());
        filterPanel.add(searchWinnerField);
        
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.setBackground(ACCENT_BLUE);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBtn.setBorder(new EmptyBorder(5, 15, 5, 15));
        searchBtn.setFocusPainted(false);
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> loadPaymentData());
        filterPanel.add(searchBtn);
        
        filterPanel.add(new JLabel("  "));
        
        JLabel statusLabel = new JLabel("📊 Status:");
        statusLabel.setForeground(TEXT_PRIMARY);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filterPanel.add(statusLabel);
        
        statusFilterCombo = new JComboBox<>(new String[]{"All", "pending", "processing", "completed", "failed", "refunded"});
        statusFilterCombo.setBackground(new Color(45, 52, 65));
        statusFilterCombo.setForeground(TEXT_PRIMARY);
        statusFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusFilterCombo.addActionListener(e -> loadPaymentData());
        filterPanel.add(statusFilterCombo);
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setBackground(ACCENT_CYAN);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setBorder(new EmptyBorder(5, 15, 5, 15));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            loadPaymentData();
            updatePaymentStats();
            addLog("🔄 Payment list refreshed");
        });
        filterPanel.add(refreshBtn);
        
        panel.add(filterPanel, BorderLayout.NORTH);
        
        // Payment Table
        String[] columns = {"ID", "Item", "Winner", "Amount", "Status", "Method", "Transaction", "Date"};
        paymentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        paymentTable = new JTable(paymentTableModel);
        paymentTable.setBackground(new Color(35, 42, 55));
        paymentTable.setForeground(TEXT_SECONDARY);
        paymentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        paymentTable.setRowHeight(32);
        paymentTable.setGridColor(new Color(45, 52, 65));
        paymentTable.setSelectionBackground(ACCENT_BLUE);
        paymentTable.setSelectionForeground(Color.WHITE);
        paymentTable.getTableHeader().setBackground(new Color(30, 37, 48));
        paymentTable.getTableHeader().setForeground(ACCENT_GOLD);
        paymentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        // Column widths
        paymentTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        paymentTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        paymentTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        paymentTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        paymentTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        paymentTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        paymentTable.getColumnModel().getColumn(6).setPreferredWidth(120);
        paymentTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(paymentTable);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(35, 42, 55));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Action Buttons Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionPanel.setBackground(BG_DARK);
        actionPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(45, 52, 65)));
        
        JButton confirmBtn = new JButton("✅ Confirm Payment");
        confirmBtn.setBackground(ACCENT_GREEN);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confirmBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBtn.addActionListener(e -> confirmSelectedPayment());
        
        JButton markFailedBtn = new JButton("❌ Mark as Failed");
        markFailedBtn.setBackground(ACCENT_RED);
        markFailedBtn.setForeground(Color.WHITE);
        markFailedBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        markFailedBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        markFailedBtn.setFocusPainted(false);
        markFailedBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        markFailedBtn.addActionListener(e -> markPaymentFailed());
        
        JButton viewDetailsBtn = new JButton("📄 View Details");
        viewDetailsBtn.setBackground(ACCENT_BLUE);
        viewDetailsBtn.setForeground(Color.WHITE);
        viewDetailsBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        viewDetailsBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        viewDetailsBtn.setFocusPainted(false);
        viewDetailsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewDetailsBtn.addActionListener(e -> viewPaymentDetails());
        
        JButton printReceiptBtn = new JButton("🖨️ Print Receipt");
        printReceiptBtn.setBackground(ACCENT_PURPLE);
        printReceiptBtn.setForeground(Color.WHITE);
        printReceiptBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        printReceiptBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
        printReceiptBtn.setFocusPainted(false);
        printReceiptBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        printReceiptBtn.addActionListener(e -> printReceipt());
        
        actionPanel.add(confirmBtn);
        actionPanel.add(markFailedBtn);
        actionPanel.add(viewDetailsBtn);
        actionPanel.add(printReceiptBtn);
        
        panel.add(actionPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSmallStatCard(String title, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 2));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accentColor, 1),
            new EmptyBorder(5, 12, 5, 12)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        titleLabel.setForeground(TEXT_SECONDARY);
        card.add(titleLabel, BorderLayout.NORTH);
        
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        valuePanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(accentColor);
        valuePanel.add(valueLabel);
        
        card.add(valuePanel, BorderLayout.CENTER);
        
        return card;
    }
    
    // ============================================================
    // ============ PAYMENT DATA METHODS ============
    // ============================================================
    
    private void loadPaymentData() {
        if (auctionService == null) return;
        
        try {
            List<PaymentRecord> payments = auctionService.getAllPayments();
            String filter = (String) statusFilterCombo.getSelectedItem();
            String search = searchWinnerField.getText().trim().toLowerCase();
            
            SwingUtilities.invokeLater(() -> {
                paymentTableModel.setRowCount(0);
                
                if (payments == null || payments.isEmpty()) {
                    paymentTableModel.addRow(new Object[]{
                        "-", "-", "-", "-", "-", "-", "-", "No payments found"
                    });
                    updatePaymentStats();
                    return;
                }
                
                for (PaymentRecord p : payments) {
                    if (!"All".equals(filter) && !p.getStatus().equalsIgnoreCase(filter)) {
                        continue;
                    }
                    
                    if (!search.isEmpty() && !p.getWinnerName().toLowerCase().contains(search)) {
                        continue;
                    }
                    
                    String statusDisplay = p.getStatusWithIcon();
                    String method = p.getPaymentMethod() != null && !p.getPaymentMethod().isEmpty() 
                                    ? p.getPaymentMethod() : "Not specified";
                    String transId = p.getTransactionId() != null && !p.getTransactionId().isEmpty() 
                                     ? p.getTransactionId() : "N/A";
                    
                    // ============ FIX: Use getFormattedCreatedAt() ============
                    String date = p.getFormattedCreatedAt();
                    
                    paymentTableModel.addRow(new Object[]{
                        p.getId(),
                        p.getItemName(),
                        p.getWinnerName(),
                        String.format("$%.2f", p.getAmount()),
                        statusDisplay,
                        method,
                        transId,
                        date
                    });
                }
                
                updatePaymentStats();
            });
            
        } catch (Exception e) {
            addLog("❌ Error loading payments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updatePaymentStats() {
        if (auctionService == null) return;
        
        try {
            double[] stats = auctionService.getPaymentStatistics();
            double total = stats[0];
            int pending = (int) stats[1];
            int processing = (int) stats[2];
            int completed = (int) stats[3];
            int failed = (int) stats[4];
            
            SwingUtilities.invokeLater(() -> {
                paymentTotalLabel.setText("$" + String.format("%.2f", total));
                paymentPendingLabel.setText(String.valueOf(pending + processing));
                paymentCompletedLabel.setText(String.valueOf(completed));
                paymentFailedLabel.setText(String.valueOf(failed));
            });
            
        } catch (Exception e) {
            // ignore
        }
    }
    
    // ============================================================
    // ============ PAYMENT ACTION METHODS ============
    // ============================================================
    
    private void confirmSelectedPayment() {
        int row = paymentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a payment to confirm!");
            return;
        }
        
        int paymentId = (int) paymentTableModel.getValueAt(row, 0);
        String status = (String) paymentTableModel.getValueAt(row, 4);
        
        if (status.contains("Completed")) {
            JOptionPane.showMessageDialog(this, "This payment is already completed!");
            return;
        }
        
        if (status.contains("Failed") || status.contains("Refunded")) {
            JOptionPane.showMessageDialog(this, "Cannot confirm a " + status + " payment!");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Confirm payment for:\n" +
            "Item: " + paymentTableModel.getValueAt(row, 1) + "\n" +
            "Winner: " + paymentTableModel.getValueAt(row, 2) + "\n" +
            "Amount: " + paymentTableModel.getValueAt(row, 3),
            "Confirm Payment",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                auctionService.confirmPaymentReceived(paymentId, "Admin");
                addLog("✅ Payment confirmed: ID=" + paymentId);
                loadPaymentData();
                JOptionPane.showMessageDialog(this, "✅ Payment confirmed successfully!");
            } catch (Exception e) {
                addLog("❌ Error confirming payment: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }
    
    private void markPaymentFailed() {
        int row = paymentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a payment!");
            return;
        }
        
        int paymentId = (int) paymentTableModel.getValueAt(row, 0);
        String status = (String) paymentTableModel.getValueAt(row, 4);
        
        if (status.contains("Completed")) {
            JOptionPane.showMessageDialog(this, "Cannot mark a completed payment as failed!");
            return;
        }
        
        if (status.contains("Failed") || status.contains("Refunded")) {
            JOptionPane.showMessageDialog(this, "This payment is already " + status + "!");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Mark this payment as FAILED?\n" +
            "Item: " + paymentTableModel.getValueAt(row, 1) + "\n" +
            "Winner: " + paymentTableModel.getValueAt(row, 2) + "\n" +
            "Amount: " + paymentTableModel.getValueAt(row, 3),
            "Mark as Failed",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                auctionService.updatePaymentStatus(paymentId, "failed", "Admin");
                addLog("❌ Payment marked as failed: ID=" + paymentId);
                loadPaymentData();
                JOptionPane.showMessageDialog(this, "Payment marked as failed.");
            } catch (Exception e) {
                addLog("❌ Error: " + e.getMessage());
            }
        }
    }
    
    private void viewPaymentDetails() {
        int row = paymentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a payment!");
            return;
        }
        
        int paymentId = (int) paymentTableModel.getValueAt(row, 0);
        
        try {
            PaymentRecord payment = auctionService.getPaymentById(paymentId);
            if (payment != null) {
                showPaymentDetailDialog(payment);
            }
        } catch (Exception e) {
            addLog("❌ Error: " + e.getMessage());
        }
    }
    
    private void printReceipt() {
        int row = paymentTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a payment!");
            return;
        }
        
        String receipt = buildReceiptString(row);
        
        JDialog receiptDialog = new JDialog(this, "📄 Payment Receipt", true);
        receiptDialog.setSize(500, 550);
        receiptDialog.setLocationRelativeTo(this);
        receiptDialog.getContentPane().setBackground(Color.WHITE);
        receiptDialog.setLayout(new BorderLayout());
        
        JTextArea receiptArea = new JTextArea(receipt);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptArea.setBackground(Color.WHITE);
        receiptArea.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        receiptDialog.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("❌ Close");
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setBorder(new EmptyBorder(8, 30, 8, 30));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> receiptDialog.dispose());
        
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        btnPanel.add(closeBtn);
        receiptDialog.add(btnPanel, BorderLayout.SOUTH);
        
        receiptDialog.setVisible(true);
    }
    
    private String buildReceiptString(int row) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════════╗\n");
        sb.append("║              PAYMENT RECEIPT                    ║\n");
        sb.append("╠══════════════════════════════════════════════════╣\n");
        sb.append("║  Receipt #: AUCTION-").append(System.currentTimeMillis()).append("\n");
        sb.append("║  Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("║                                                    \n");
        sb.append("║  Winner: ").append(padRight(paymentTableModel.getValueAt(row, 2).toString(), 20)).append("\n");
        sb.append("║  Item: ").append(padRight(paymentTableModel.getValueAt(row, 1).toString(), 22)).append("\n");
        sb.append("║  Amount: ").append(padRight(paymentTableModel.getValueAt(row, 3).toString(), 20)).append("\n");
        sb.append("║  Status: ").append(padRight(paymentTableModel.getValueAt(row, 4).toString(), 20)).append("\n");
        sb.append("║  Method: ").append(padRight(paymentTableModel.getValueAt(row, 5).toString(), 20)).append("\n");
        sb.append("║  Transaction: ").append(paymentTableModel.getValueAt(row, 6)).append("\n");
        sb.append("║                                                    \n");
        sb.append("║  🎉 Thank you for your purchase!                   \n");
        sb.append("╚══════════════════════════════════════════════════╝\n");
        return sb.toString();
    }
    
    private String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }
    
    private void showPaymentDetailDialog(PaymentRecord payment) {
        JDialog dialog = new JDialog(this, "💳 Payment Details", true);
        dialog.setSize(450, 550);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("💳 PAYMENT DETAILS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(ACCENT_GOLD);
        mainPanel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        addDetailRow(mainPanel, gbc, "ID:", String.valueOf(payment.getId()));
        gbc.gridy = 2;
        addDetailRow(mainPanel, gbc, "Item:", payment.getItemName());
        gbc.gridy = 3;
        addDetailRow(mainPanel, gbc, "Winner:", payment.getWinnerName());
        gbc.gridy = 4;
        addDetailRow(mainPanel, gbc, "Amount:", "$" + String.format("%.2f", payment.getAmount()));
        gbc.gridy = 5;
        addDetailRow(mainPanel, gbc, "Status:", payment.getStatusIcon() + " " + payment.getStatusDisplay());
        gbc.gridy = 6;
        addDetailRow(mainPanel, gbc, "Method:", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A");
        gbc.gridy = 7;
        addDetailRow(mainPanel, gbc, "Transaction:", payment.getTransactionId() != null ? payment.getTransactionId() : "N/A");
        gbc.gridy = 8;
        addDetailRow(mainPanel, gbc, "Date:", payment.getCreatedAt() != null ? 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(payment.getCreatedAt()) : "N/A");
        gbc.gridy = 9;
        addDetailRow(mainPanel, gbc, "Confirmed By:", payment.getConfirmedBy() != null ? payment.getConfirmedBy() : "N/A");
        gbc.gridy = 10;
        addDetailRow(mainPanel, gbc, "Confirmed At:", payment.getConfirmedAt() != null ? 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(payment.getConfirmedAt()) : "N/A");
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(BG_DARK);
        
        JButton closeBtn = new JButton("❌ Close");
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setBorder(new EmptyBorder(8, 30, 8, 30));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void addDetailRow(JPanel panel, GridBagConstraints gbc, String label, String value) {
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelComp.setForeground(ACCENT_GOLD);
        gbc.gridx = 0;
        panel.add(labelComp, gbc);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        valueComp.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1;
        panel.add(valueComp, gbc);
    }
    
    // ============================================================
    // ============ EXISTING METHODS ============
    // ============================================================
    
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void startServer() {
        new Thread(() -> {
            try {
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                addLog("🚀 Starting RMI Registry on port " + RMI_PORT + "...");
                try {
                    LocateRegistry.createRegistry(RMI_PORT);
                    addLog("✅ RMI Registry started successfully on port " + RMI_PORT);
                } catch (Exception e) {
                    addLog("⚠️ Registry already exists on port " + RMI_PORT + " - using existing");
                }
                
                addLog("🔧 Creating Auction Service instance...");
                auctionService = new AuctionServiceImpl();
                
                String url = "rmi://localhost:" + RMI_PORT + "/AuctionService";
                addLog("🔗 Binding service to: " + url);
                Naming.rebind(url, auctionService);
                
                String ip = InetAddress.getLocalHost().getHostAddress();
                SwingUtilities.invokeLater(() -> {
                    ipLabel.setText(ip);
                    addItemButton.setEnabled(true);
                    createScheduleButton.setEnabled(true);
                    toggleScheduledModeButton.setEnabled(true);
                });
                
                statusLabel.setText("● RUNNING");
                statusLabel.setForeground(new Color(100, 255, 100));
                serverStarted = true;
                
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                addLog("🎉 SERVER STARTED SUCCESSFULLY!");
                addLog("📡 Server IP Address: " + ip);
                addLog("🔌 RMI Port Number: " + RMI_PORT);
                addLog("💾 Database: SQLite (auction.db)");
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                addLog("");
                addLog("💡 Quick Actions:");
                addLog("   ➕ Click 'ADD NEW ITEM' to add auction items");
                addLog("   📅 Click 'CREATE SCHEDULE' to schedule auction start times");
                addLog("   🔘 Click 'ENABLE SCHEDULED MODE' to activate scheduling");
                addLog("   💳 View payments in 'PAYMENTS' tab");
                addLog("");
                addLog("👥 Waiting for clients to connect...");
                addLog("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                SwingUtilities.invokeLater(() -> {
                    loadPaymentData();
                    updatePaymentStats();
                });
                
            } catch (Exception e) {
                statusLabel.setText("● ERROR");
                statusLabel.setForeground(Color.RED);
                addLog("❌ SERVER ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private void toggleScheduledMode() {
        if (auctionService == null) return;
        
        try {
            scheduledModeEnabled = !scheduledModeEnabled;
            auctionService.enableScheduledMode(scheduledModeEnabled);
            
            if (scheduledModeEnabled) {
                scheduledModeLabel.setText("ENABLED");
                scheduledModeLabel.setForeground(ACCENT_GREEN);
                toggleScheduledModeButton.setText("🔴 DISABLE SCHEDULED MODE");
                toggleScheduledModeButton.setBackground(new Color(200, 50, 50));
                addLog("📅 SCHEDULED MODE: ENABLED - Auctions will start at scheduled times");
            } else {
                scheduledModeLabel.setText("DISABLED");
                scheduledModeLabel.setForeground(ACCENT_RED);
                toggleScheduledModeButton.setText("🟢 ENABLE SCHEDULED MODE");
                toggleScheduledModeButton.setBackground(new Color(0, 123, 255));
                addLog("📅 SCHEDULED MODE: DISABLED - Auctions will start immediately");
            }
        } catch (Exception e) {
            addLog("❌ Failed to toggle scheduled mode: " + e.getMessage());
        }
    }
    
    private void showAddItemDialog() {
        JDialog dialog = new JDialog(this, "➕ Add New Auction Item", true);
        dialog.setSize(650, 700);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_CARD);
        formPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("📦 Item Name:");
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(nameLabel, gbc);
        
        JTextField nameField = new JTextField(20);
        nameField.setBackground(new Color(45, 52, 65));
        nameField.setForeground(TEXT_PRIMARY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel descLabel = new JLabel("📝 Description:");
        descLabel.setForeground(TEXT_PRIMARY);
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(descLabel, gbc);
        
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setBackground(new Color(45, 52, 65));
        descArea.setForeground(TEXT_PRIMARY);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        descArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBackground(new Color(45, 52, 65));
        gbc.gridx = 1;
        formPanel.add(descScroll, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel priceLabel = new JLabel("💰 Starting Price ($):");
        priceLabel.setForeground(TEXT_PRIMARY);
        priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(priceLabel, gbc);
        
        JTextField priceField = new JTextField(20);
        priceField.setBackground(new Color(45, 52, 65));
        priceField.setForeground(TEXT_PRIMARY);
        priceField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        formPanel.add(priceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel imageLabel = new JLabel("🖼️ Item Image:");
        imageLabel.setForeground(TEXT_PRIMARY);
        imageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(imageLabel, gbc);
        
        JPanel imagePanel = new JPanel(new BorderLayout(10, 10));
        imagePanel.setBackground(BG_CARD);
        
        JLabel imagePreviewLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(150, 150));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(new Color(80, 90, 110), 2));
        imagePreviewLabel.setBackground(new Color(35, 42, 55));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setForeground(TEXT_SECONDARY);
        
        JButton selectImageBtn = new JButton("📁 Select Image");
        selectImageBtn.setBackground(new Color(0, 123, 255));
        selectImageBtn.setForeground(Color.WHITE);
        selectImageBtn.setBorder(new EmptyBorder(8, 15, 8, 15));
        selectImageBtn.setFocusPainted(false);
        selectImageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton clearImageBtn = new JButton("🗑️ Clear");
        clearImageBtn.setBackground(new Color(108, 117, 125));
        clearImageBtn.setForeground(Color.WHITE);
        clearImageBtn.setBorder(new EmptyBorder(8, 15, 8, 15));
        clearImageBtn.setFocusPainted(false);
        clearImageBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setBackground(BG_CARD);
        btnPanel.add(selectImageBtn);
        btnPanel.add(clearImageBtn);
        
        imagePanel.add(imagePreviewLabel, BorderLayout.CENTER);
        imagePanel.add(btnPanel, BorderLayout.SOUTH);
        
        gbc.gridx = 1;
        formPanel.add(imagePanel, gbc);
        
        final byte[][] selectedImageData = new byte[1][0];
        final String[] selectedFileName = new String[1];
        
        selectImageBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser("images");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "jpg", "jpeg", "png", "gif"));
            
            int result = fileChooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    BufferedImage img = ImageIO.read(selectedFile);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                        imagePreviewLabel.setIcon(new ImageIcon(scaled));
                        imagePreviewLabel.setText("");
                        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(img, "jpg", baos);
                        selectedImageData[0] = baos.toByteArray();
                        selectedFileName[0] = selectedFile.getName();
                        addLog("📷 Image selected: " + selectedFileName[0]);
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Could not read image file!");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error loading image: " + ex.getMessage());
                }
            }
        });
        
        clearImageBtn.addActionListener(e -> {
            imagePreviewLabel.setIcon(null);
            imagePreviewLabel.setText("No image selected");
            imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
            selectedImageData[0] = null;
            selectedFileName[0] = null;
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(BG_DARK);
        
        JButton addBtn = new JButton("✅ Add Item");
        addBtn.setBackground(new Color(40, 167, 69));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton cancelBtn = new JButton("❌ Cancel");
        cancelBtn.setBackground(new Color(108, 117, 125));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String desc = descArea.getText().trim();
            String priceStr = priceField.getText().trim();
            
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter item name!");
                return;
            }
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter description!");
                return;
            }
            
            double price;
            try {
                price = Double.parseDouble(priceStr);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid price!");
                return;
            }
            
            try {
                if (auctionService != null) {
                    String fileName = selectedFileName[0] != null ? selectedFileName[0] : name.toLowerCase().replace(" ", "_") + ".jpg";
                    auctionService.addNewItemWithImage(name, desc, price, selectedImageData[0], fileName);
                    addLog("✅ New item added: " + name + " - $" + price);
                    JOptionPane.showMessageDialog(dialog, "✅ Item added successfully!");
                    dialog.dispose();
                }
            } catch (Exception ex) {
                addLog("❌ Error adding item: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }
    
    private void showCreateScheduleDialog() {
        JDialog dialog = new JDialog(this, "📅 Create Auction Schedule", true);
        dialog.setSize(550, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_CARD);
        formPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel itemLabel = new JLabel("🎯 Select Item:");
        itemLabel.setForeground(TEXT_PRIMARY);
        itemLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(itemLabel, gbc);
        
        JComboBox<String> itemCombo = new JComboBox<>();
        itemCombo.setBackground(new Color(45, 52, 65));
        itemCombo.setForeground(TEXT_PRIMARY);
        itemCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 90, 110), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        try {
            if (auctionService != null) {
                List<AuctionItem> items = auctionService.getItems();
                for (AuctionItem item : items) {
                    if (!item.isSold()) {
                        itemCombo.addItem(item.getId() + " - " + item.getName() + " ($" + item.getStartingPrice() + ")");
                    }
                }
            }
        } catch (Exception e) {
            addLog("Error loading items: " + e.getMessage());
        }
        
        if (itemCombo.getItemCount() == 0) {
            itemCombo.addItem("No items available - Add items first");
        }
        
        gbc.gridx = 1;
        formPanel.add(itemCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel dateLabel = new JLabel("📅 Start Date:");
        dateLabel.setForeground(TEXT_PRIMARY);
        dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(dateLabel, gbc);
        
        JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setBackground(new Color(45, 52, 65));
        dateSpinner.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1;
        formPanel.add(dateSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel timeLabel = new JLabel("⏰ Start Time:");
        timeLabel.setForeground(TEXT_PRIMARY);
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(timeLabel, gbc);
        
        JSpinner timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm:ss");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setBackground(new Color(45, 52, 65));
        timeSpinner.setForeground(TEXT_PRIMARY);
        gbc.gridx = 1;
        formPanel.add(timeSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel durationLabel = new JLabel("⏱️ Duration:");
        durationLabel.setForeground(TEXT_PRIMARY);
        durationLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(durationLabel, gbc);
        
        JLabel durationValueLabel = new JLabel("120 seconds (2 minutes) - Fixed");
        durationValueLabel.setForeground(ACCENT_GOLD);
        durationValueLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbc.gridx = 1;
        formPanel.add(durationValueLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(45, 52, 65));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_GOLD, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        
        JLabel infoTitle = new JLabel("ℹ️ Schedule Information");
        infoTitle.setForeground(ACCENT_GOLD);
        infoTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        infoPanel.add(infoTitle);
        infoPanel.add(Box.createVerticalStrut(5));
        
        JLabel infoText = new JLabel("• Auction will start at the specified date and time");
        infoText.setForeground(TEXT_SECONDARY);
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoPanel.add(infoText);
        
        JLabel infoText2 = new JLabel("• Duration is fixed at 2 minutes (120 seconds)");
        infoText2.setForeground(TEXT_SECONDARY);
        infoText2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoPanel.add(infoText2);
        
        JLabel infoText3 = new JLabel("• All connected clients will be notified");
        infoText3.setForeground(TEXT_SECONDARY);
        infoText3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoPanel.add(infoText3);
        
        formPanel.add(infoPanel, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(BG_DARK);
        
        JButton createBtn = new JButton("✅ Create Schedule");
        createBtn.setBackground(new Color(40, 167, 69));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        createBtn.setFocusPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton cancelBtn = new JButton("❌ Cancel");
        cancelBtn.setBackground(new Color(108, 117, 125));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(createBtn);
        buttonPanel.add(cancelBtn);
        
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        createBtn.addActionListener(e -> {
            if (itemCombo.getItemCount() == 0 || itemCombo.getItemAt(0).equals("No items available - Add items first")) {
                JOptionPane.showMessageDialog(dialog, "No items available for scheduling!\nPlease add items first.");
                return;
            }
            
            String selected = (String) itemCombo.getSelectedItem();
            int itemId = Integer.parseInt(selected.split(" - ")[0]);
            
            Date selectedDate = (Date) dateSpinner.getValue();
            Date selectedTime = (Date) timeSpinner.getValue();
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(selectedDate);
            java.util.Calendar timeCal = java.util.Calendar.getInstance();
            timeCal.setTime(selectedTime);
            
            cal.set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY));
            cal.set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE));
            cal.set(java.util.Calendar.SECOND, timeCal.get(java.util.Calendar.SECOND));
            
            Date startTime = cal.getTime();
            
            if (startTime.getTime() <= System.currentTimeMillis()) {
                JOptionPane.showMessageDialog(dialog, "Start time must be in the future!");
                return;
            }
            
            try {
                auctionService.createAuctionSchedule(itemId, startTime);
                addLog("📅 Schedule created for item ID " + itemId + " at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
                JOptionPane.showMessageDialog(dialog, "✅ Schedule created successfully!\nAuction will start at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime));
                dialog.dispose();
            } catch (Exception ex) {
                addLog("❌ Error creating schedule: " + ex.getMessage());
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });
        
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        dialog.setVisible(true);
    }
    
    private void refreshAll() {
        updateStats();
        updateClientList();
        loadPaymentData();
        updatePaymentStats();
        addLog("🔄 Manual refresh completed");
    }
    
    private void startTimers() {
        statsTimer = new Timer(2000, e -> updateStats());
        statsTimer.start();
        
        clientListTimer = new Timer(3000, e -> updateClientList());
        clientListTimer.start();
        
        Timer paymentTimer = new Timer(5000, e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                loadPaymentData();
                updatePaymentStats();
            }
        });
        paymentTimer.start();
    }
    
    private void updateStats() {
        if (auctionService != null && serverStarted) {
            try {
                List<AuctionItem> items = auctionService.getItems();
                if (items != null) {
                    int activeCount = 0;
                    AuctionItem currentItem = null;
                    
                    for (AuctionItem item : items) {
                        if (item != null && item.isActive()) {
                            activeCount++;
                            currentItem = item;
                        }
                    }
                    activeAuctionsLabel.setText(String.valueOf(activeCount));
                    
                    if (currentItem != null) {
                        currentItemLabel.setText(currentItem.getName());
                    } else {
                        currentItemLabel.setText("Auction Ended");
                    }
                }
                
                List<String> clients = auctionService.getConnectedClients();
                if (clients != null) {
                    clientCountLabel.setText(String.valueOf(clients.size()));
                }
                
                scheduledModeEnabled = auctionService.isScheduledModeEnabled();
                if (scheduledModeEnabled) {
                    scheduledModeLabel.setText("ENABLED");
                    scheduledModeLabel.setForeground(ACCENT_GREEN);
                } else {
                    scheduledModeLabel.setText("DISABLED");
                    scheduledModeLabel.setForeground(ACCENT_RED);
                }
                
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
    
    private void updateClientList() {
        if (auctionService != null && serverStarted) {
            try {
                List<String> clients = auctionService.getConnectedClients();
                SwingUtilities.invokeLater(() -> {
                    clientTableModel.setRowCount(0);
                    if (clients != null) {
                        int count = 1;
                        for (String clientName : clients) {
                            if (clientName != null) {
                                clientTableModel.addRow(new Object[]{
                                    count++,
                                    clientName,
                                    "🟢 Connected",
                                    new SimpleDateFormat("HH:mm:ss").format(new Date())
                                });
                            }
                        }
                    }
                });
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            UIManager.put("Button.arc", 12);
            UIManager.put("ToolTip.background", BG_CARD);
            UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            AuctionServerGUI server = new AuctionServerGUI();
            server.setVisible(true);
        });
    }
}