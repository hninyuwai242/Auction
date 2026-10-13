package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private static final String DB_NAME = "auction.db";
    private static Connection connection = null;
    
    public static synchronized Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
                System.out.println("✅ Database connected successfully!");
                createTables();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC Driver not found!");
            throw new SQLException("SQLite JDBC Driver not found.", e);
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            throw e;
        }
        return connection;
    }
    
    private static void createTables() {
        String sqlItems = """
            CREATE TABLE IF NOT EXISTS auction_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                starting_price REAL NOT NULL,
                current_price REAL NOT NULL,
                current_winner TEXT,
                end_time INTEGER,
                icon TEXT,
                image_data BLOB,
                image_file_name TEXT,
                sold INTEGER DEFAULT 0,
                winner_name TEXT,
                active INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String sqlHistory = """
            CREATE TABLE IF NOT EXISTS auction_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                item_name TEXT NOT NULL,
                winner_name TEXT NOT NULL,
                final_price REAL NOT NULL,
                sold_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (item_id) REFERENCES auction_items(id)
            )
        """;
        
        String sqlBids = """
            CREATE TABLE IF NOT EXISTS bid_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                bidder_name TEXT NOT NULL,
                bid_amount REAL NOT NULL,
                bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (item_id) REFERENCES auction_items(id)
            )
        """;
        
        String sqlSchedules = """
            CREATE TABLE IF NOT EXISTS auction_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                item_name TEXT NOT NULL,
                starting_price REAL NOT NULL,
                scheduled_start_time INTEGER NOT NULL,
                scheduled_end_time INTEGER NOT NULL,
                status TEXT DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (item_id) REFERENCES auction_items(id)
            )
        """;
        
        String sqlPayments = """
            CREATE TABLE IF NOT EXISTS payment_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id INTEGER NOT NULL,
                item_name TEXT NOT NULL,
                winner_name TEXT NOT NULL,
                amount REAL NOT NULL,
                status TEXT DEFAULT 'pending',
                payment_method TEXT,
                transaction_id TEXT,
                payment_date TIMESTAMP,
                confirmed_by TEXT,
                confirmed_at TIMESTAMP,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (item_id) REFERENCES auction_items(id)
            )
        """;
        
        String sqlPaymentMethods = """
            CREATE TABLE IF NOT EXISTS payment_methods (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                method_name TEXT NOT NULL,
                is_active INTEGER DEFAULT 1,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlItems);
            stmt.execute(sqlHistory);
            stmt.execute(sqlBids);
            stmt.execute(sqlSchedules);
            stmt.execute(sqlPayments);
            stmt.execute(sqlPaymentMethods);
            System.out.println("✅ Database tables created/verified successfully");
            
            insertDefaultPaymentMethods();
            
        } catch (SQLException e) {
            System.err.println("❌ Error creating tables: " + e.getMessage());
        }
    }
    
    private static void insertDefaultPaymentMethods() {
        String checkSql = "SELECT COUNT(*) FROM payment_methods";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = """
                    INSERT INTO payment_methods (method_name, description) VALUES
                    ('Credit Card', 'Visa, Mastercard, Amex'),
                    ('Bank Transfer', 'Direct bank transfer'),
                    ('E-Wallet', 'PayPal, GCash, etc.'),
                    ('Cash', 'Cash payment in person')
                """;
                stmt.execute(insertSql);
                System.out.println("✅ Default payment methods inserted");
            }
        } catch (SQLException e) {
            // Ignore
        }
    }
    
    // ============= ITEM METHODS =============
    
    public static int insertItem(AuctionItem item) throws SQLException {
        String sql = """
            INSERT INTO auction_items (
                name, description, starting_price, current_price, current_winner,
                end_time, icon, image_data, image_file_name, sold, winner_name, active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getName());
            pstmt.setString(2, item.getDescription());
            pstmt.setDouble(3, item.getStartingPrice());
            pstmt.setDouble(4, item.getCurrentPrice());
            pstmt.setString(5, item.getCurrentWinner());
            pstmt.setLong(6, item.getEndTime());
            pstmt.setString(7, item.getIcon());
            pstmt.setBytes(8, item.getImageData());
            pstmt.setString(9, item.getImageFileName());
            pstmt.setInt(10, item.isSold() ? 1 : 0);
            pstmt.setString(11, item.getWinnerName());
            pstmt.setInt(12, item.isActive() ? 1 : 0);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
    
    public static List<AuctionItem> getAllItems() throws SQLException {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM auction_items ORDER BY id";
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapToItem(rs));
            }
        }
        return items;
    }
    
    public static AuctionItem getItemById(int id) throws SQLException {
        String sql = "SELECT * FROM auction_items WHERE id = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapToItem(rs);
            }
        }
        return null;
    }
    
    public static void updateItem(AuctionItem item) throws SQLException {
        String sql = """
            UPDATE auction_items SET 
                current_price = ?, current_winner = ?, end_time = ?, 
                sold = ?, winner_name = ?, active = ?
            WHERE id = ?
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, item.getCurrentPrice());
            pstmt.setString(2, item.getCurrentWinner());
            pstmt.setLong(3, item.getEndTime());
            pstmt.setInt(4, item.isSold() ? 1 : 0);
            pstmt.setString(5, item.getWinnerName());
            pstmt.setInt(6, item.isActive() ? 1 : 0);
            pstmt.setInt(7, item.getId());
            pstmt.executeUpdate();
        }
    }
    
    public static void updateItemActiveStatus(int itemId, boolean active) throws SQLException {
        String sql = "UPDATE auction_items SET active = ? WHERE id = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, itemId);
            pstmt.executeUpdate();
        }
    }
    
    public static void deleteItem(int itemId) throws SQLException {
        String sql = "DELETE FROM auction_items WHERE id = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.executeUpdate();
        }
    }
    
    private static AuctionItem mapToItem(ResultSet rs) throws SQLException {
        AuctionItem item = new AuctionItem(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getDouble("starting_price"),
            rs.getBytes("image_data"),
            rs.getString("image_file_name")
        );
        item.setCurrentPrice(rs.getDouble("current_price"));
        item.setCurrentWinner(rs.getString("current_winner"));
        item.setEndTime(rs.getLong("end_time"));
        item.setIcon(rs.getString("icon"));
        item.setSold(rs.getInt("sold") == 1);
        item.setWinnerName(rs.getString("winner_name"));
        item.setActive(rs.getInt("active") == 1);
        return item;
    }
    
    // ============= BID HISTORY METHODS =============
    
    public static void insertBidHistory(int itemId, String bidderName, double amount) throws SQLException {
        String sql = "INSERT INTO bid_history (item_id, bidder_name, bid_amount) VALUES (?, ?, ?)";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setString(2, bidderName);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        }
    }
    
    public static List<String[]> getBidHistory(int itemId) throws SQLException {
        List<String[]> history = new ArrayList<>();
        String sql = "SELECT bidder_name, bid_amount, bid_time FROM bid_history WHERE item_id = ? ORDER BY bid_time DESC";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(new String[]{
                    rs.getString("bidder_name"),
                    String.format("%.2f", rs.getDouble("bid_amount")),
                    rs.getString("bid_time")
                });
            }
        }
        return history;
    }
    
    // ============= AUCTION HISTORY METHODS =============
    
    public static void insertAuctionHistory(int itemId, String itemName, String winnerName, double finalPrice) throws SQLException {
        String sql = "INSERT INTO auction_history (item_id, item_name, winner_name, final_price) VALUES (?, ?, ?, ?)";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            pstmt.setString(2, itemName);
            pstmt.setString(3, winnerName);
            pstmt.setDouble(4, finalPrice);
            pstmt.executeUpdate();
        }
    }
    
    public static List<String[]> getAuctionHistory() throws SQLException {
        List<String[]> history = new ArrayList<>();
        String sql = "SELECT item_name, winner_name, final_price, sold_at FROM auction_history ORDER BY sold_at DESC";
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                history.add(new String[]{
                    rs.getString("item_name"),
                    rs.getString("winner_name"),
                    String.format("%.2f", rs.getDouble("final_price")),
                    rs.getString("sold_at")
                });
            }
        }
        return history;
    }
    
    // ============= SCHEDULE METHODS =============
    
    public static int insertSchedule(AuctionSchedule schedule) throws SQLException {
        String sql = """
            INSERT INTO auction_schedules (
                item_id, item_name, starting_price, scheduled_start_time, scheduled_end_time, status
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, schedule.getItemId());
            pstmt.setString(2, schedule.getItemName());
            pstmt.setDouble(3, schedule.getStartingPrice());
            pstmt.setLong(4, schedule.getScheduledStartTime().getTime());
            pstmt.setLong(5, schedule.getScheduledEndTime().getTime());
            pstmt.setString(6, schedule.getStatus());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
    
    public static List<AuctionSchedule> getAllSchedules() throws SQLException {
        List<AuctionSchedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM auction_schedules ORDER BY scheduled_start_time";
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                AuctionSchedule schedule = new AuctionSchedule();
                schedule.setId(rs.getInt("id"));
                schedule.setItemId(rs.getInt("item_id"));
                schedule.setItemName(rs.getString("item_name"));
                schedule.setStartingPrice(rs.getDouble("starting_price"));
                schedule.setScheduledStartTime(new java.util.Date(rs.getLong("scheduled_start_time")));
                schedule.setScheduledEndTime(new java.util.Date(rs.getLong("scheduled_end_time")));
                schedule.setStatus(rs.getString("status"));
                schedules.add(schedule);
            }
        }
        return schedules;
    }
    
    public static void updateScheduleStatus(int scheduleId, String status) throws SQLException {
        String sql = "UPDATE auction_schedules SET status = ? WHERE id = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, scheduleId);
            pstmt.executeUpdate();
        }
    }
    
    public static void updateScheduleStatusByItemId(int itemId, String status) throws SQLException {
        String sql = "UPDATE auction_schedules SET status = ? WHERE item_id = ? AND status = 'pending'";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, itemId);
            pstmt.executeUpdate();
        }
    }
    
    // ============================================================
    // ============= PAYMENT METHODS =============
    // ============================================================
    
    public static int insertPaymentRecord(PaymentRecord payment) throws SQLException {
        String sql = """
            INSERT INTO payment_records (
                item_id, item_name, winner_name, amount, status,
                payment_method, transaction_id, payment_date, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, payment.getItemId());
            pstmt.setString(2, payment.getItemName());
            pstmt.setString(3, payment.getWinnerName());
            pstmt.setDouble(4, payment.getAmount());
            pstmt.setString(5, payment.getStatus() != null ? payment.getStatus() : "pending");
            pstmt.setString(6, payment.getPaymentMethod());
            pstmt.setString(7, payment.getTransactionId());
            
            if (payment.getPaymentDate() != null) {
                pstmt.setLong(8, payment.getPaymentDate().getTime());
            } else {
                pstmt.setNull(8, java.sql.Types.BIGINT);
            }
            
            pstmt.setString(9, payment.getNotes());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }
    
    public static List<PaymentRecord> getAllPaymentRecords() throws SQLException {
        List<PaymentRecord> payments = new ArrayList<>();
        String sql = "SELECT * FROM payment_records ORDER BY created_at DESC";
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapToPayment(rs));
            }
        }
        return payments;
    }
    
    public static PaymentRecord getPaymentRecordById(int id) throws SQLException {
        String sql = "SELECT * FROM payment_records WHERE id = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapToPayment(rs);
            }
        }
        return null;
    }
    
    public static PaymentRecord getPaymentRecordByItemId(int itemId) throws SQLException {
        String sql = "SELECT * FROM payment_records WHERE item_id = ? ORDER BY created_at DESC LIMIT 1";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapToPayment(rs);
            }
        }
        return null;
    }
    
    public static void updatePaymentStatus(int paymentId, String status, String confirmedBy) throws SQLException {
        String sql = """
            UPDATE payment_records SET 
                status = ?, 
                confirmed_by = ?, 
                confirmed_at = ?
            WHERE id = ?
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, confirmedBy);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.setInt(4, paymentId);
            pstmt.executeUpdate();
        }
    }
    
    // ============================================================
    // ============= FIXED: updatePaymentDetails =============
    // ============================================================
    
    public static void updatePaymentDetails(int paymentId, String paymentMethod, String transactionId, java.util.Date paymentDate) throws SQLException {
        // ============ FIX: Only update payment_date if it's not null ============
        // If paymentDate is null, keep the existing payment_date
        String sql;
        if (paymentDate != null) {
            sql = """
                UPDATE payment_records SET 
                    payment_method = ?,
                    transaction_id = ?,
                    payment_date = ?
                WHERE id = ?
            """;
        } else {
            // Don't update payment_date - keep existing value
            sql = """
                UPDATE payment_records SET 
                    payment_method = ?,
                    transaction_id = ?
                WHERE id = ?
            """;
        }
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (paymentDate != null) {
                pstmt.setString(1, paymentMethod);
                pstmt.setString(2, transactionId);
                pstmt.setLong(3, paymentDate.getTime());
                pstmt.setInt(4, paymentId);
                System.out.println("📝 Saving payment_date: " + paymentDate + " (timestamp: " + paymentDate.getTime() + ")");
            } else {
                pstmt.setString(1, paymentMethod);
                pstmt.setString(2, transactionId);
                pstmt.setInt(3, paymentId);
                System.out.println("📝 Updating payment details (keeping existing payment_date)");
            }
            
            int rows = pstmt.executeUpdate();
            System.out.println("✅ Updated " + rows + " row(s)");
        }
    }
    
    public static boolean isPaymentCompleted(int itemId) throws SQLException {
        String sql = "SELECT status FROM payment_records WHERE item_id = ? ORDER BY created_at DESC LIMIT 1";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                return "completed".equals(status);
            }
        }
        return false;
    }
    
    public static double getTotalPaymentAmount() throws SQLException {
        String sql = "SELECT SUM(amount) FROM payment_records WHERE status = 'completed'";
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
    
    public static int getPaymentCountByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payment_records WHERE status = ?";
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    // ============================================================
    // ============= PAYMENT HISTORY METHODS =============
    // ============================================================
    
    public static List<String[]> getPaymentHistory() throws SQLException {
        List<String[]> history = new ArrayList<>();
        String sql = """
            SELECT id, item_name, winner_name, amount, status, 
                   payment_method, transaction_id, created_at 
            FROM payment_records 
            ORDER BY created_at DESC
        """;
        
        Connection conn = getConnection();
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                history.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("item_name"),
                    rs.getString("winner_name"),
                    String.format("%.2f", rs.getDouble("amount")),
                    rs.getString("status"),
                    rs.getString("payment_method") != null ? rs.getString("payment_method") : "-",
                    rs.getString("transaction_id") != null ? rs.getString("transaction_id") : "-",
                    rs.getString("created_at")
                });
            }
        }
        return history;
    }
    
    public static List<String[]> getPaymentHistoryByItem(int itemId) throws SQLException {
        List<String[]> history = new ArrayList<>();
        String sql = """
            SELECT id, item_name, winner_name, amount, status, 
                   payment_method, transaction_id, created_at 
            FROM payment_records 
            WHERE item_id = ?
            ORDER BY created_at DESC
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("item_name"),
                    rs.getString("winner_name"),
                    String.format("%.2f", rs.getDouble("amount")),
                    rs.getString("status"),
                    rs.getString("payment_method") != null ? rs.getString("payment_method") : "-",
                    rs.getString("transaction_id") != null ? rs.getString("transaction_id") : "-",
                    rs.getString("created_at")
                });
            }
        }
        return history;
    }
    
    public static List<String[]> getPaymentHistoryByWinner(String winnerName) throws SQLException {
        List<String[]> history = new ArrayList<>();
        String sql = """
            SELECT id, item_name, winner_name, amount, status, 
                   payment_method, transaction_id, created_at 
            FROM payment_records 
            WHERE winner_name LIKE ?
            ORDER BY created_at DESC
        """;
        
        Connection conn = getConnection();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + winnerName + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("item_name"),
                    rs.getString("winner_name"),
                    String.format("%.2f", rs.getDouble("amount")),
                    rs.getString("status"),
                    rs.getString("payment_method") != null ? rs.getString("payment_method") : "-",
                    rs.getString("transaction_id") != null ? rs.getString("transaction_id") : "-",
                    rs.getString("created_at")
                });
            }
        }
        return history;
    }
    
    private static PaymentRecord mapToPayment(ResultSet rs) throws SQLException {
        PaymentRecord payment = new PaymentRecord();
        payment.setId(rs.getInt("id"));
        payment.setItemId(rs.getInt("item_id"));
        payment.setItemName(rs.getString("item_name"));
        payment.setWinnerName(rs.getString("winner_name"));
        payment.setAmount(rs.getDouble("amount"));
        payment.setStatus(rs.getString("status"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setTransactionId(rs.getString("transaction_id"));
        
        // ============ FIX: Read payment_date correctly ============
        long paymentDate = rs.getLong("payment_date");
        System.out.println("📖 Reading payment_date from DB: " + paymentDate);
        if (paymentDate > 0) {
            payment.setPaymentDate(new java.util.Date(paymentDate));
            System.out.println("📖 Converted to: " + payment.getPaymentDate());
        } else {
            payment.setPaymentDate(null);
            System.out.println("📖 payment_date is null or 0 - keeping as null");
        }
        
        payment.setConfirmedBy(rs.getString("confirmed_by"));
        
        long confirmedAt = rs.getLong("confirmed_at");
        if (confirmedAt > 0) {
            payment.setConfirmedAt(new java.util.Date(confirmedAt));
        }
        
        payment.setNotes(rs.getString("notes"));
        
        long createdAt = rs.getLong("created_at");
        if (createdAt > 0) {
            payment.setCreatedAt(new java.util.Date(createdAt));
        }
        
        return payment;
    }
    
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing database: " + e.getMessage());
        }
    }
}