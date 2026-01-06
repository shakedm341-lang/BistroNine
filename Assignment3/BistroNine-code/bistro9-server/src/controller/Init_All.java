package controller;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Database Initialization class.
 * Final Version:
 * 1. Historical & Future data.
 * 2. Historical Waiting List data (Seated/Cancelled).
 * 3. Waiting list for NOW.
 * 4. Live Dining for NOW.
 */
public class Init_All {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static final String USER = "root";
    private static final String PASSWORD = "Aa123456"; 

    private static final Random random = new Random();

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection con = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                 Statement stmt = con.createStatement()) {

                System.out.println("Starting database initialization...");

                dropExistingTables(con, stmt);
                createTables(con, stmt);

                initDiscounts(con, stmt);
                initOpeningHours(con, stmt);
                initSpecialHours(con, stmt);
                initTables(con, stmt);
                initUsersAndSubscribers(con);
                
                // 1. היסטוריה ועתיד (כולל רשימת המתנה היסטורית)
                initReservationsAndBills(con);
                
                // 2. רשימת המתנה (עכשיו)
                initWaitingListForToday(con);

                // 3. סועדים בפועל (עכשיו)
                initLiveDiningForNow(con);
                
                initReports(con, stmt);

                System.out.println("Initialization completed successfully.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("ERROR - DATABASE INITIALIZATION FAILED");
        }
    }

    // --- 1. Drop Tables ---
    private static void dropExistingTables(Connection con, Statement stmt) {
        try {
            System.out.println("Dropping existing tables...");
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            String[] tables = {
                "subscriber_report", "time_report", "report_manager", "waiting_list",
                "bills", "restaurant_discount", "special_hours", "weekly_hours",
                "table_reservations", "restaurant_tables", "subscriber", "customer"
            };
            for (String table : tables) {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
            }
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("All existing tables dropped.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 2. Create Tables ---
    private static void createTables(Connection con, Statement stmt) {
        try {
            System.out.println("Creating tables...");
            stmt.executeUpdate("CREATE TABLE customer (customerId INT AUTO_INCREMENT, phoneNumber VARCHAR(100) UNIQUE, email VARCHAR(100) UNIQUE, CONSTRAINT check_contact_info CHECK (phoneNumber IS NOT NULL OR email IS NOT NULL), PRIMARY KEY (customerId));");
            stmt.executeUpdate("CREATE TABLE subscriber (subscriberId INT AUTO_INCREMENT, customerId INT NOT NULL UNIQUE, firstName VARCHAR(100) NOT NULL, lastName VARCHAR(100) NOT NULL, type ENUM('subscriber', 'restaurant representative', 'restaurant manager') NOT NULL, personalInfo VARCHAR(1000), username VARCHAR(100) NOT NULL UNIQUE, password VARCHAR(100) NOT NULL, PRIMARY KEY (subscriberId), FOREIGN KEY (customerId) REFERENCES customer(customerId) ON UPDATE CASCADE);");
            stmt.executeUpdate("CREATE TABLE restaurant_tables (tableId INT AUTO_INCREMENT, seatsNumber INT NOT NULL, location ENUM('inside', 'bar', 'outside') NOT NULL, status ENUM('available', 'occupied', 'cancelled') NOT NULL, PRIMARY KEY (tableId));");
            stmt.executeUpdate("CREATE TABLE table_reservations (reservationId INT AUTO_INCREMENT, tableId INT, numberOfDiners INT NOT NULL, confirmationCode INT NOT NULL UNIQUE, customerId INT NOT NULL, reservationDate DATETIME NOT NULL, dateOfMakeReservation DATETIME DEFAULT CURRENT_TIMESTAMP, arrivalTime DATETIME, leavingTime DATETIME, status ENUM('active','arrived' ,'cancelled', 'completed','waiting' ) NOT NULL DEFAULT 'active', PRIMARY KEY (reservationId), FOREIGN KEY (customerId) REFERENCES customer(customerId) ON UPDATE CASCADE, FOREIGN KEY (tableId) REFERENCES restaurant_tables(tableId));");
            stmt.executeUpdate("CREATE TABLE weekly_hours (dayOfWeek ENUM('SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY') NOT NULL, openingTime TIME NOT NULL, closingTime TIME NOT NULL, PRIMARY KEY (dayOfWeek, openingTime));");
            stmt.executeUpdate("CREATE TABLE special_hours (specificDate DATE NOT NULL, openingTime TIME NOT NULL, closingTime TIME NOT NULL, PRIMARY KEY (specificDate, openingTime));");
            stmt.executeUpdate("CREATE TABLE restaurant_discount (type_customer ENUM('subscriber', 'customer'), discount DECIMAL(5,2) DEFAULT 0.00, PRIMARY KEY (type_customer));");
            stmt.executeUpdate("CREATE TABLE bills (billId INT AUTO_INCREMENT, reservationId INT NOT NULL, totalAmount DECIMAL(10, 2) DEFAULT 0.00, totalAmountAfterDiscount DECIMAL(10, 2) DEFAULT 0.00, discountPercentage DECIMAL(5,2) DEFAULT 0.00, isPaid BOOLEAN DEFAULT FALSE, discountType ENUM('subscriber', 'customer') DEFAULT NULL, paymentMethod ENUM('cash', 'credit', 'app') DEFAULT NULL, PRIMARY KEY (billId), FOREIGN KEY (reservationId) REFERENCES table_reservations(reservationId));");
            stmt.executeUpdate("CREATE TABLE waiting_list (waitingId INT NOT NULL AUTO_INCREMENT, reservationId INT NOT NULL, numberOfDiners INT NOT NULL, entryTimeToList TIMESTAMP DEFAULT CURRENT_TIMESTAMP, exitTimeFromList TIMESTAMP, status ENUM('waiting', 'seated', 'cancelled','notified') DEFAULT 'waiting', type ENUM('walk_in', 'check_in') NOT NULL, PRIMARY KEY (waitingId), FOREIGN KEY (reservationId) REFERENCES table_reservations(reservationId));");
            stmt.executeUpdate("CREATE TABLE report_manager (reportId INT AUTO_INCREMENT, startDay DATE NOT NULL, endDay DATE NOT NULL, generatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP, reportRange ENUM('monthly', 'weekly','daily') NOT NULL, reportType ENUM('time', 'subscriber') NOT NULL, PRIMARY KEY (reportId), UNIQUE KEY unique_report_range (startDay, endDay, reportType));");
            stmt.executeUpdate("CREATE TABLE time_report (reportId INT NOT NULL, reportDate DATE NOT NULL, avgArrival INT NOT NULL, avgLeaving INT NOT NULL, PRIMARY KEY (reportId, reportDate), FOREIGN KEY (reportId) REFERENCES report_manager(reportId) ON DELETE CASCADE);");
            stmt.executeUpdate("CREATE TABLE subscriber_report (reportId INT NOT NULL, reportDate DATE NOT NULL, totalReservations INT NOT NULL, totalWaiting INT NOT NULL, PRIMARY KEY (reportId, reportDate), FOREIGN KEY (reportId) REFERENCES report_manager(reportId) ON DELETE CASCADE);");
            System.out.println("Tables created successfully.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 3. Init Data ---
    private static void initDiscounts(Connection con, Statement stmt) {
        try {
            stmt.executeUpdate("INSERT INTO restaurant_discount (type_customer, discount) VALUES ('subscriber', 10.00)");
            stmt.executeUpdate("INSERT INTO restaurant_discount (type_customer, discount) VALUES ('customer', 0.00)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void initOpeningHours(Connection con, Statement stmt) {
        try {
            String[] weekDays = { "MONDAY", "WEDNESDAY", "THURSDAY" };
            for (String day : weekDays) {
                stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('" + day + "', '08:00:00', '23:00:00')");
            }
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('SUNDAY', '00:00:00', '03:00:00')");
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('SUNDAY', '08:00:00', '23:00:00')");
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('TUESDAY', '08:00:00', '12:00:00')");
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('TUESDAY', '16:00:00', '23:00:00')");
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('FRIDAY', '08:00:00', '14:00:00')");
            stmt.executeUpdate("INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES ('SATURDAY', '20:00:00', '23:59:59')");
            System.out.println("Inserted weekly hours.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void initSpecialHours(Connection con, Statement stmt) {
        try {
            // Dec
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2025-12-24', '10:00:00', '14:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2025-12-25', '00:00:00', '00:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2025-12-31', '18:00:00', '23:59:59')");
            // Jan
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-01', '00:00:00', '04:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-01', '13:00:00', '22:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-02', '16:00:00', '23:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-10', '00:00:00', '00:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-15', '10:00:00', '23:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-20', '11:00:00', '15:00:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-20', '19:30:00', '23:30:00')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-01-25', '09:00:00', '14:00:00')");
            // Other
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2026-02-14', '09:00:00', '23:59:59')");
            stmt.executeUpdate("INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES ('2025-11-01', '12:00:00', '20:00:00')");
            System.out.println("Inserted special hours.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void initTables(Connection con, Statement stmt) {
        try {
            for (int i = 1; i <= 10; i++) {
                int seats = (i % 3 == 0) ? 6 : ((i % 3 == 2) ? 4 : 2);
                String loc = (i <= 4) ? "inside" : ((i <= 8) ? "outside" : "bar");
                stmt.executeUpdate("INSERT INTO restaurant_tables (seatsNumber, location, status) VALUES (" + seats + ", '" + loc + "', 'available')");
            }
            System.out.println("Inserted tables.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 4. Users ---
    private static void initUsersAndSubscribers(Connection con) {
        String[] firstNames = { "Or", "Adi", "Shaked", "Bob", "John", "Jennifer", "Michael", "Linda", "David", "Sarah" };
        String[] lastNames = { "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Wilson", "Moore" };
        
        String insertCust = "INSERT INTO customer (phoneNumber, email) VALUES (?, ?)";
        String insertSub = "INSERT INTO subscriber (customerId, firstName, lastName, type, username, password) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psCust = con.prepareStatement(insertCust, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psSub = con.prepareStatement(insertSub)) {

            // Manager
            psCust.setString(1, "0500000001");
            psCust.setString(2, "manager@rest.com");
            psCust.executeUpdate();
            ResultSet rs = psCust.getGeneratedKeys();
            if (rs.next()) {
                int custId = rs.getInt(1);
                psSub.setInt(1, custId);
                psSub.setString(2, "Boss");
                psSub.setString(3, "Man");
                psSub.setString(4, "restaurant manager");
                psSub.setString(5, "manager");
                psSub.setString(6, "1234");
                psSub.executeUpdate();
            }

            // Representative
            psCust.setString(1, "0500000002");
            psCust.setString(2, "rep@rest.com");
            psCust.executeUpdate();
            rs = psCust.getGeneratedKeys();
            if (rs.next()) {
                int custId = rs.getInt(1);
                psSub.setInt(1, custId);
                psSub.setString(2, "Ron");
                psSub.setString(3, "Levy");
                psSub.setString(4, "restaurant representative"); 
                psSub.setString(5, "employee"); 
                psSub.setString(6, "1234");     
                psSub.executeUpdate();
            }

            // Subscribers
            for (int i = 0; i < 10; i++) {
                psCust.setString(1, "050123456" + i);
                psCust.setString(2, firstNames[i].toLowerCase() + "@mail.com");
                psCust.executeUpdate();
                rs = psCust.getGeneratedKeys();
                if (rs.next()) {
                    int custId = rs.getInt(1);
                    psSub.setInt(1, custId);
                    psSub.setString(2, firstNames[i]);
                    psSub.setString(3, lastNames[i]);
                    psSub.setString(4, "subscriber");
                    psSub.setString(5, firstNames[i].toLowerCase()); 
                    psSub.setString(6, "1"); 
                    psSub.executeUpdate();
                }
            }

            // Regular Customers
            for (int k = 1; k <= 3; k++) {
                psCust.setString(1, "054111111" + k);
                psCust.setString(2, "full_guest_" + k + "@gmail.com");
                psCust.executeUpdate();
            }
            for (int k = 1; k <= 3; k++) {
                psCust.setString(1, "054222222" + k);
                psCust.setNull(2, java.sql.Types.VARCHAR);
                psCust.executeUpdate();
            }
            for (int k = 1; k <= 3; k++) {
                psCust.setNull(1, java.sql.Types.VARCHAR);
                psCust.setString(2, "email_only_guest_" + k + "@gmail.com");
                psCust.executeUpdate();
            }
            System.out.println("Inserted all user types.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 5. Reservations (History/Future) + Historical Waiting List ---
    private static void initReservationsAndBills(Connection con) {
        String selectCust = "SELECT customerId FROM customer";
        String insertRes = "INSERT INTO table_reservations (tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String insertBill = "INSERT INTO bills (reservationId, totalAmount, totalAmountAfterDiscount, discountPercentage, isPaid, discountType, paymentMethod) VALUES (?, ?, ?, ?, true, ?, 'credit')";
        // SQL עבור רשימת המתנה היסטורית
        String insertWaitHistory = "INSERT INTO waiting_list (reservationId, numberOfDiners, type, status, entryTimeToList, exitTimeFromList) VALUES (?, ?, ?, ?, ?, ?)";

        try (Statement stmt = con.createStatement();
             PreparedStatement psRes = con.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBill = con.prepareStatement(insertBill);
             PreparedStatement psWaitHistory = con.prepareStatement(insertWaitHistory)) {

            List<Integer> custIds = new ArrayList<>();
            ResultSet rs = stmt.executeQuery(selectCust);
            while (rs.next()) custIds.add(rs.getInt(1));

            System.out.println("Generating reservations & historical waiting list...");
            int confCode = 5000;
            
            // Dec
            for (int day = 1; day <= 31; day++) {
                createDailyReservations(con, psRes, psBill, psWaitHistory, custIds, LocalDate.of(2025, 12, day), confCode);
                confCode += 20; 
            }
            // Jan
            for (int day = 1; day <= 31; day++) {
                createDailyReservations(con, psRes, psBill, psWaitHistory, custIds, LocalDate.of(2026, 1, day), confCode);
                confCode += 20;
            }
            System.out.println("Reservations & Historical Waiting List populated.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 6. Waiting List (Close to NOW) ---
    private static void initWaitingListForToday(Connection con) {
        String selectCust = "SELECT customerId FROM customer";
        String insertRes = "INSERT INTO table_reservations (customerId, numberOfDiners, reservationDate, confirmationCode, status, tableId) VALUES (?, ?, ?, ?, 'waiting', NULL)";
        String insertWait = "INSERT INTO waiting_list (reservationId, numberOfDiners, type, status, entryTimeToList) VALUES (?, ?, ?, 'waiting', ?)";

        try (Statement stmt = con.createStatement();
             PreparedStatement psRes = con.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psWait = con.prepareStatement(insertWait)) {

            List<Integer> custIds = new ArrayList<>();
            ResultSet rs = stmt.executeQuery(selectCust);
            while (rs.next()) custIds.add(rs.getInt(1));

            LocalDateTime now = LocalDateTime.now();
            System.out.println("Generating Waiting List for TODAY...");
            
            if (isDayClosed(con, now.toLocalDate())) {
                 System.out.println("Restaurant closed today, skipping waiting list.");
                 return;
            }

            int confCode = 9000; 

            for (int i = 0; i < 5; i++) {
                int custId = custIds.get(random.nextInt(custIds.size()));
                int diners = 2 + random.nextInt(4);
                
                int minutesAgo = random.nextInt(60); 
                LocalDateTime entryTime = now.minusMinutes(minutesAgo);

                psRes.setInt(1, custId);
                psRes.setInt(2, diners);
                psRes.setTimestamp(3, Timestamp.valueOf(entryTime));
                psRes.setInt(4, confCode++);
                psRes.executeUpdate();

                ResultSet rsRes = psRes.getGeneratedKeys();
                if (rsRes.next()) {
                    int resId = rsRes.getInt(1);
                    psWait.setInt(1, resId);
                    psWait.setInt(2, diners);
                    String type = random.nextBoolean() ? "walk_in" : "check_in";
                    psWait.setString(3, type);
                    psWait.setTimestamp(4, Timestamp.valueOf(entryTime));
                    psWait.executeUpdate();
                }
            }
            System.out.println("Waiting List populated.");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- 7. Live Dining ---
    private static void initLiveDiningForNow(Connection con) {
        String selectCust = "SELECT customerId FROM customer";
        String insertRes = "INSERT INTO table_reservations (tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime, status) VALUES (?, ?, ?, ?, ?, ?, NULL, 'arrived')";
        String updateTableStatus = "UPDATE restaurant_tables SET status = 'occupied' WHERE tableId = ?";
        String insertBill = "INSERT INTO bills (reservationId, totalAmount, totalAmountAfterDiscount, discountPercentage, isPaid, discountType, paymentMethod) VALUES (?, ?, ?, ?, false, ?, NULL)";

        try (Statement stmt = con.createStatement();
             PreparedStatement psRes = con.prepareStatement(insertRes, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psTable = con.prepareStatement(updateTableStatus);
             PreparedStatement psBill = con.prepareStatement(insertBill)) {

            List<Integer> custIds = new ArrayList<>();
            ResultSet rs = stmt.executeQuery(selectCust);
            while (rs.next()) custIds.add(rs.getInt(1));

            LocalDateTime now = LocalDateTime.now();
            System.out.println("Generating LIVE DINING...");

            if (isDayClosed(con, now.toLocalDate())) {
                 System.out.println("Restaurant closed today, skipping live dining.");
                 return;
            }

            int confCode = 9500; 
            List<Integer> usedTables = new ArrayList<>();
            int liveTables = 3 + random.nextInt(3);

            for (int i = 0; i < liveTables; i++) {
                int custId = custIds.get(random.nextInt(custIds.size()));
                int diners = 2 + random.nextInt(4);
                int tableId;
                do {
                    tableId = 1 + random.nextInt(10);
                } while (usedTables.contains(tableId));
                usedTables.add(tableId);

                int minutesAgo = 30 + random.nextInt(60); 
                LocalDateTime arrivalTime = now.minusMinutes(minutesAgo);

                psRes.setInt(1, tableId);
                psRes.setInt(2, diners);
                psRes.setInt(3, confCode++);
                psRes.setInt(4, custId);
                psRes.setTimestamp(5, Timestamp.valueOf(arrivalTime));
                psRes.setTimestamp(6, Timestamp.valueOf(arrivalTime));
                psRes.executeUpdate();

                psTable.setInt(1, tableId);
                psTable.executeUpdate();

                ResultSet rsRes = psRes.getGeneratedKeys();
                if (rsRes.next()) {
                    int resId = rsRes.getInt(1);
                    double amount = 150.0 + random.nextInt(200);
                    boolean isSubscriberBill = random.nextBoolean();
                    double discountPercent = isSubscriberBill ? 10.0 : 0.0;
                    double finalAmount = isSubscriberBill ? (amount * 0.9) : amount;
                    String type = isSubscriberBill ? "subscriber" : "customer";

                    psBill.setInt(1, resId);
                    psBill.setDouble(2, amount);
                    psBill.setDouble(3, finalAmount);
                    psBill.setDouble(4, discountPercent);
                    psBill.setString(5, type);
                    psBill.executeUpdate();
                }
            }
            System.out.println("Live Dining populated (" + liveTables + " tables).");

        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Helper Methods ---

    private static LocalTime getRandomOpenTime(Connection con, LocalDate date) {
        List<LocalTime[]> shifts = new ArrayList<>();
        String dayOfWeek = date.getDayOfWeek().toString();

        String specialQuery = "SELECT openingTime, closingTime FROM special_hours WHERE specificDate = ?";
        try (PreparedStatement ps = con.prepareStatement(specialQuery)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time open = rs.getTime("openingTime");
                    Time close = rs.getTime("closingTime");
                    if (open.equals(close)) return null; 
                    shifts.add(new LocalTime[]{open.toLocalTime(), close.toLocalTime()});
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (shifts.isEmpty()) {
            String weeklyQuery = "SELECT openingTime, closingTime FROM weekly_hours WHERE dayOfWeek = ?";
            try (PreparedStatement ps = con.prepareStatement(weeklyQuery)) {
                ps.setString(1, dayOfWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Time open = rs.getTime("openingTime");
                        Time close = rs.getTime("closingTime");
                        shifts.add(new LocalTime[]{open.toLocalTime(), close.toLocalTime()});
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        if (shifts.isEmpty()) return null;

        LocalTime[] chosenShift = shifts.get(random.nextInt(shifts.size()));
        LocalTime open = chosenShift[0];
        LocalTime close = chosenShift[1];

        long minutesOpen = ChronoUnit.MINUTES.between(open, close);
        if (minutesOpen <= 60) return open; 

        long randomMinute = random.nextInt((int) minutesOpen - 60); 
        return open.plusMinutes(randomMinute);
    }

    private static boolean isDayClosed(Connection con, LocalDate date) {
        return getRandomOpenTime(con, date) == null;
    }

    private static void createDailyReservations(Connection con, PreparedStatement psRes, PreparedStatement psBill, PreparedStatement psWaitHistory, List<Integer> custIds, LocalDate date, int startConfCode) throws SQLException {
        if (isDayClosed(con, date)) {
            return; 
        }

        int dailyRes = 8 + random.nextInt(5); 
        int currentCode = startConfCode;
        boolean isFutureDate = date.isAfter(LocalDate.now());

        for (int i = 0; i < dailyRes; i++) {
            int custId = custIds.get(random.nextInt(custIds.size()));
            int diners = 2 + random.nextInt(4);
            
            LocalTime randomTime = getRandomOpenTime(con, date);
            if (randomTime == null) continue;
            
            LocalDateTime scheduledTime = LocalDateTime.of(date, randomTime);
            
            psRes.setInt(2, diners);
            psRes.setInt(3, currentCode++);
            psRes.setInt(4, custId);
            psRes.setTimestamp(5, Timestamp.valueOf(scheduledTime));

            if (isFutureDate) {
                // עתיד - Active
                psRes.setNull(1, java.sql.Types.INTEGER); 
                psRes.setNull(6, java.sql.Types.TIMESTAMP); 
                psRes.setNull(7, java.sql.Types.TIMESTAMP); 
                psRes.setString(8, "active");
                psRes.executeUpdate();
            } else {
                // היסטוריה - Completed / Cancelled
                int tableId = 1 + random.nextInt(10); 
                psRes.setInt(1, tableId); 
                int arrivalOffset = random.nextInt(31) - 15;
                LocalDateTime actualArrival = scheduledTime.plusMinutes(arrivalOffset);
                int durationVariance = random.nextInt(61) - 30;
                LocalDateTime actualLeaving = actualArrival.plusHours(2).plusMinutes(durationVariance);
                
                boolean isCancelled = random.nextDouble() < 0.20; 
                String status = isCancelled ? "cancelled" : "completed";
                
                psRes.setTimestamp(6, Timestamp.valueOf(actualArrival));
                psRes.setTimestamp(7, Timestamp.valueOf(actualLeaving));
                psRes.setString(8, status);
                psRes.executeUpdate();
                
                ResultSet rsRes = psRes.getGeneratedKeys();
                if (rsRes.next()) {
                    int resId = rsRes.getInt(1);

                    // --- לוגיקה חדשה: רשימת המתנה היסטורית ---
                    // סיכוי של 30% שהלקוח הזה היה ברשימת המתנה לפני שנכנס (או ביטל)
                    if (random.nextDouble() < 0.30) {
                        int waitDuration = 5 + random.nextInt(40); // חיכה בין 5 ל-45 דקות
                        LocalDateTime entryToList = actualArrival.minusMinutes(waitDuration);
                        LocalDateTime exitFromList = actualArrival; // יצא כשהתיישב או ביטל
                        
                        String waitStatus = "seated";
                        String type = random.nextBoolean() ? "walk_in" : "check_in";

                        if (isCancelled) {
                            waitStatus = "cancelled"; // התייאש וביטל
                            // אם ביטל, יצא מהרשימה בזמן הביטול המשוער
                            exitFromList = entryToList.plusMinutes(waitDuration);
                        }

                        psWaitHistory.setInt(1, resId);
                        psWaitHistory.setInt(2, diners);
                        psWaitHistory.setString(3, type);
                        psWaitHistory.setString(4, waitStatus);
                        psWaitHistory.setTimestamp(5, Timestamp.valueOf(entryToList));
                        psWaitHistory.setTimestamp(6, Timestamp.valueOf(exitFromList));
                        psWaitHistory.executeUpdate();
                    }
                    // ---------------------------------------------

                    if (!isCancelled) {
                        double amount = 150.0 + random.nextInt(200);
                        boolean isSubscriberBill = random.nextBoolean();
                        double discountPercent = isSubscriberBill ? 10.0 : 0.0;
                        double finalAmount = isSubscriberBill ? (amount * 0.9) : amount;
                        String type = isSubscriberBill ? "subscriber" : "customer";
                        psBill.setInt(1, resId);
                        psBill.setDouble(2, amount);
                        psBill.setDouble(3, finalAmount);
                        psBill.setDouble(4, discountPercent);
                        psBill.setString(5, type);
                        psBill.executeUpdate();
                    }
                }
            }
        }
    }

    // --- 8. Reports ---
    private static void initReports(Connection con, Statement stmt) {
        try {
            stmt.executeUpdate("INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES ('2025-11-01', '2025-11-30', 'monthly', 'time')", Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int reportId = rs.getInt(1);
                for (int day = 1; day <= 30; day++) {
                    int avgArr = random.nextInt(20);
                    int avgLev = random.nextInt(20);
                    stmt.executeUpdate(String.format("INSERT INTO time_report (reportId, reportDate, avgArrival, avgLeaving) VALUES (%d, '2025-11-%02d', %d, %d)", reportId, day, avgArr, avgLev));
                }
            }
            stmt.executeUpdate("INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES ('2025-11-01', '2025-11-30', 'monthly', 'subscriber')", Statement.RETURN_GENERATED_KEYS);
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int reportId = rs.getInt(1);
                for (int day = 1; day <= 30; day++) {
                    int totalRes = 10 + random.nextInt(30);
                    int totalWait = random.nextInt(10);
                    stmt.executeUpdate(String.format("INSERT INTO subscriber_report (reportId, reportDate, totalReservations, totalWaiting) VALUES (%d, '2025-11-%02d', %d, %d)", reportId, day, totalRes, totalWait));
                }
            }
            System.out.println("Initialized Nov reports.");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}