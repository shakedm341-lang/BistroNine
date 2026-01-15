package controller;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Init_All {

    // כתובת לחיבור לשרת (לצורך יצירת ה-DB מחדש)
    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    // כתובת לחיבור ל-DB הספציפי (לאחר שנוצר)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    
    private static final String USER = "root";
    // סיסמה ברירת מחדל לשימוש ב-main, אך ב-GUI תתקבל סיסמה מהמשתמש
    private static final String DEFAULT_PASSWORD = "Aa123456"; 

    private static final Random random = new Random();

    // Main לשימוש בדיקות עצמאיות (ללא GUI)
    public static void main(String[] args) {
        runInitialization(DEFAULT_PASSWORD);
    }

    /**
     * New entry point for the GUI "Screen Zero".
     * @param dbPassword The password provided by the user in the GUI.
     * @return true if initialization was successful, false otherwise.
     */
    public static boolean runInitialization(String dbPassword) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 1. Connect to the Server directly to Drop and Create the DB
            try (Connection serverCon = DriverManager.getConnection(SERVER_URL, USER, dbPassword);
                 Statement serverStmt = serverCon.createStatement()) {

                System.out.println("Connected to MySQL server. Resetting database 'restaurant_db'...");
                
                // Drop the database if it exists
                serverStmt.executeUpdate("DROP DATABASE IF EXISTS restaurant_db");
                
                // Create the database fresh
                serverStmt.executeUpdate("CREATE DATABASE restaurant_db");
                
                System.out.println("Database created successfully.");
            }

            // 2. Connect to the newly created restaurant_db to initialize content
            try (Connection con = DriverManager.getConnection(DB_URL, USER, dbPassword);
                 Statement stmt = con.createStatement()) {

                System.out.println("Starting table and data initialization...");

                // createTables מחליף את הצורך ב-dropExistingTables כי מחקנו את כל ה-DB
                createTables(con, stmt);
                initDiscounts(con, stmt);
                initOpeningHours(con, stmt);
                initSpecialHours(con, stmt);
                initTables(con, stmt);
                initUsersAndSubscribers(con);
                
                initReservationsAndBills(con);
                initLiveDiningForNow(con);
                
                initReports(con, stmt);

                System.out.println("Initialization completed successfully.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("SQL Error during initialization: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createTables(Connection con, Statement stmt) throws SQLException {
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
    }

    private static void initDiscounts(Connection con, Statement stmt) throws SQLException {
        stmt.executeUpdate("INSERT INTO restaurant_discount (type_customer, discount) VALUES ('subscriber', 10.00), ('customer', 0.00)");
    }

    private static void initOpeningHours(Connection con, Statement stmt) throws SQLException {
        String[] days = {"SUNDAY", "MONDAY", "WEDNESDAY", "THURSDAY"};
        for (String d : days) stmt.executeUpdate("INSERT INTO weekly_hours VALUES ('" + d + "', '08:00:00', '23:00:00')");
        stmt.executeUpdate("INSERT INTO weekly_hours VALUES ('TUESDAY', '08:00:00', '12:00:00'), ('TUESDAY', '16:00:00', '23:00:00'), ('FRIDAY', '08:00:00', '14:00:00'), ('SATURDAY', '20:00:00', '01:00:00')");
    }

    private static void initSpecialHours(Connection con, Statement stmt) throws SQLException {
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2025-12-25', '00:00:00', '00:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-01', '00:00:00', '00:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-15', '08:00:00', '14:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-20', '19:00:00', '00:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-28', '00:00:00', '00:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-25', '08:00:00', '12:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-01-25', '16:00:00', '23:00:00')");
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-02-14', '10:00:00', '23:59:00')"); 
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-04-01', '08:00:00', '13:00:00')"); 
        stmt.executeUpdate("INSERT INTO special_hours VALUES ('2026-04-22', '00:00:00', '00:00:00')"); 
    }

    private static void initTables(Connection con, Statement stmt) throws SQLException {
        for (int i = 1; i <= 10; i++) {
            int seats = (i % 3 == 0) ? 6 : ((i % 3 == 2) ? 4 : 2);
            stmt.executeUpdate("INSERT INTO restaurant_tables (seatsNumber, location, status) VALUES (" + seats + ", 'inside', 'available')");
        }
    }

    private static void initUsersAndSubscribers(Connection con) throws SQLException {
        String[] firstNames = {"Dan", "Noa", "Omer", "Maya", "Idan", "Gal", "Yael", "Tom", "Ron", "Adi"};
        String[] lastNames = {"Cohen", "Levi", "Mizrahi", "Peretz", "Biton", "Dahan", "Katz", "Azulai", "Golan", "Friedman"};

        String insertCust = "INSERT INTO customer (phoneNumber, email) VALUES (?, ?)";
        String insertSub = "INSERT INTO subscriber (customerId, firstName, lastName, type, username, password) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psCust = con.prepareStatement(insertCust, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psSub = con.prepareStatement(insertSub)) {

            // 1. Manager
            psCust.setString(1, "0500000001");
            psCust.setString(2, "manager@rest.com");
            psCust.executeUpdate();
            ResultSet rs = psCust.getGeneratedKeys();
            if (rs.next()) {
                psSub.setInt(1, rs.getInt(1));
                psSub.setString(2, "Moti"); psSub.setString(3, "Manager"); psSub.setString(4, "restaurant manager");
                psSub.setString(5, "man"); psSub.setString(6, "1");
                psSub.executeUpdate();
            }

            // 2. Representative
            psCust.setString(1, "0500000002");
            psCust.setString(2, "rep@rest.com");
            psCust.executeUpdate();
            rs = psCust.getGeneratedKeys();
            if (rs.next()) {
                psSub.setInt(1, rs.getInt(1));
                psSub.setString(2, "Sarah"); psSub.setString(3, "Service"); psSub.setString(4, "restaurant representative");
                psSub.setString(5, "rep"); psSub.setString(6, "1");
                psSub.executeUpdate();
            }

            // 3. Customers
            for (int i = 0; i < 10; i++) {
                psCust.setString(1, "05012345" + i);
                psCust.setString(2, firstNames[i % firstNames.length] + i + "@mail.com");
                psCust.executeUpdate();
                rs = psCust.getGeneratedKeys();
                if (rs.next()) {
                    psSub.setInt(1, rs.getInt(1));
                    psSub.setString(2, firstNames[i % firstNames.length]); 
                    psSub.setString(3, lastNames[i % lastNames.length]);
                    psSub.setString(4, "subscriber");
                    psSub.setString(5, firstNames[i % firstNames.length]);
                    psSub.setString(6, "1");
                    psSub.executeUpdate();
                }
            }
        }
        System.out.println("Users created: 1 Manager, 1 Representative, and 10 Subscribers with real names.");
    }

    private static void initReservationsAndBills(Connection con) throws SQLException {
        List<Integer> custIds = new ArrayList<>();
        ResultSet rs = con.createStatement().executeQuery("SELECT customerId FROM customer");
        while (rs.next()) custIds.add(rs.getInt(1));

        String resSql = "INSERT INTO table_reservations (tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String billSql = "INSERT INTO bills (reservationId, totalAmount, totalAmountAfterDiscount, discountPercentage, isPaid, discountType, paymentMethod) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String waitSql = "INSERT INTO waiting_list (reservationId, numberOfDiners, type, status, entryTimeToList, exitTimeFromList) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psRes = con.prepareStatement(resSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBill = con.prepareStatement(billSql);
             PreparedStatement psWaitHistory = con.prepareStatement(waitSql)) {

            int confCode = 5000;
            LocalDate startDate = LocalDate.of(2025, 12, 1);
            LocalDate endDate = LocalDate.now().plusMonths(1);
            long daysToCreate = ChronoUnit.DAYS.between(startDate, endDate);

            for (int i = 0; i <= daysToCreate; i++) { 
                createDailyReservations(con, psRes, psBill, psWaitHistory, custIds, startDate.plusDays(i), confCode);
                confCode += 30; 
            }
        }
        System.out.println("Reservations created from 2025-12-01 until one month from now.");
    }

    private static void createDailyReservations(Connection con, PreparedStatement psRes, PreparedStatement psBill, PreparedStatement psWaitHistory, List<Integer> custIds, LocalDate date, int startConfCode) throws SQLException {
        if (isDayClosed(con, date) || date.equals(LocalDate.now())) return;

        boolean isFutureDate = date.isAfter(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();

        List<LocalDate> peakDates = Arrays.asList(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 25));
        boolean isPeakDay = peakDates.contains(date);

        // 1. Peak Days - יוצר 10 הזמנות בדיוק
        if (isPeakDay) {
            LocalTime peakHour;
            boolean isFriday = date.getDayOfWeek() == java.time.DayOfWeek.FRIDAY;
            boolean isSaturday = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY;
            boolean isEarlyClosingDate = date.equals(LocalDate.of(2026, 1, 15)) || date.equals(LocalDate.of(2026, 4, 1));
            boolean isLateOpeningDate = date.equals(LocalDate.of(2026, 1, 20));

            if (isFriday || isEarlyClosingDate) {
                peakHour = LocalTime.of(12, 0); 
            } else if (isSaturday || isLateOpeningDate) {
                peakHour = LocalTime.of(21, 0);
            } else {
                peakHour = LocalTime.of(19, 0);
            }

            for (int i = 0; i < 10; i++) {
                int custId = custIds.get(random.nextInt(custIds.size()));
                LocalDateTime scheduledTime = LocalDateTime.of(date, peakHour);
                psRes.setInt(1, i + 1); psRes.setInt(2, 2); psRes.setInt(3, startConfCode++); psRes.setInt(4, custId);
                psRes.setTimestamp(5, Timestamp.valueOf(scheduledTime));
                
                try {
                    if (scheduledTime.isBefore(now)) {
                         psRes.setTimestamp(6, Timestamp.valueOf(scheduledTime));
                         psRes.setTimestamp(7, Timestamp.valueOf(scheduledTime.plusHours(2)));
                         psRes.setString(8, "completed");
                         psRes.executeUpdate();
                         createBill(psBill, psRes, true);
                    } else {
                         psRes.setNull(6, java.sql.Types.TIMESTAMP); psRes.setNull(7, java.sql.Types.TIMESTAMP);
                         psRes.setString(8, "active");
                         psRes.executeUpdate();
                    }
                } catch (SQLException e) {
                    System.out.println("Caught overlap/error during Peak Day generation: " + e.getMessage() + ". Skipping.");
                }
            }
        }

        // 2. Scattered Reservations
        // אם זה יום עמוס - 0 הזמנות נוספות. אחרת רגיל.
        int dailyRes = isPeakDay ? 0 : (isFutureDate ? 6 : (8 + random.nextInt(5)));
        int currentCode = startConfCode;

        for (int i = 0; i < dailyRes; i++) {
            LocalTime rTime = getRandomOpenTime(con, date);
            if (rTime == null) continue;
            
            LocalDateTime scheduled = LocalDateTime.of(date, rTime);
            int custId = custIds.get(random.nextInt(custIds.size()));
            int diners = 2 + random.nextInt(4);

            LocalDateTime actualArrival;
            LocalDateTime actualLeaving;
            double arrChance = random.nextDouble();
            double leaveChance = random.nextDouble();

            if (arrChance < 0.20) actualArrival = scheduled.minusMinutes(15 + random.nextInt(31));
            else if (arrChance < 0.40) actualArrival = scheduled.plusMinutes(15 + random.nextInt(16));
            else if (arrChance < 0.70) actualArrival = scheduled.plusMinutes(random.nextInt(5) - 2);
            else actualArrival = scheduled.plusMinutes(random.nextInt(21) - 10);

            long durationMinutes;
            if (leaveChance < 0.20) durationMinutes = 150 + random.nextInt(61);
            else if (leaveChance < 0.40) durationMinutes = 45 + random.nextInt(31);
            else if (leaveChance < 0.70) durationMinutes = 120;
            else durationMinutes = 105 + random.nextInt(31);

            actualLeaving = actualArrival.plusMinutes(durationMinutes);

            boolean isFutureTime = scheduled.isAfter(now);
            boolean isOngoing = !isFutureTime && actualLeaving.isAfter(now); 

            psRes.setInt(2, diners);
            psRes.setInt(3, currentCode++);
            psRes.setInt(4, custId);
            psRes.setTimestamp(5, Timestamp.valueOf(scheduled));

            try {
                if (isFutureTime) {
                    psRes.setNull(1, java.sql.Types.INTEGER);
                    psRes.setNull(6, java.sql.Types.TIMESTAMP); psRes.setNull(7, java.sql.Types.TIMESTAMP);
                    psRes.setString(8, "active");
                    psRes.executeUpdate();
                } else if (isOngoing) {
                     continue; 
                } else {
                    boolean cancelled = random.nextDouble() < 0.2; 
                    psRes.setInt(1, 1 + random.nextInt(10));
                    psRes.setTimestamp(6, Timestamp.valueOf(actualArrival)); psRes.setTimestamp(7, Timestamp.valueOf(actualLeaving));
                    psRes.setString(8, cancelled ? "cancelled" : "completed");
                    psRes.executeUpdate();

                    if (!cancelled) createBill(psBill, psRes, true);

                    if (!date.equals(LocalDate.now()) && random.nextDouble() < 0.3) {
                        ResultSet rs = psRes.getGeneratedKeys();
                        if (rs.next()) {
                            int rid = rs.getInt(1);
                            psWaitHistory.setInt(1, rid);
                            psWaitHistory.setInt(2, diners);
                            psWaitHistory.setString(3, "walk_in");
                            psWaitHistory.setString(4, cancelled ? "cancelled" : "seated");
                            psWaitHistory.setTimestamp(5, Timestamp.valueOf(actualArrival.minusMinutes(15)));
                            psWaitHistory.setTimestamp(6, Timestamp.valueOf(actualArrival));
                            psWaitHistory.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                 System.out.println("Caught overlap/error during Daily generation: " + e.getMessage() + ". Skipping.");
            }
        }
    }
    
    private static void createBill(PreparedStatement psBill, PreparedStatement psRes, boolean isPaid) throws SQLException {
        ResultSet rs = psRes.getGeneratedKeys();
        if (rs.next()) {
            psBill.setInt(1, rs.getInt(1));
            psBill.setDouble(2, 200.0);
            psBill.setDouble(3, 180.0);
            psBill.setDouble(4, 10.0);
            psBill.setBoolean(5, isPaid);
            psBill.setString(6, "subscriber");
            psBill.setString(7, "credit");
            psBill.executeUpdate();
        }
    }

    private static void initLiveDiningForNow(Connection con) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        int[] tables = {1, 2, 3, 4}; 
        
        List<Integer> validCustomerIds = new ArrayList<>();
        try (Statement s = con.createStatement(); 
             ResultSet rs = s.executeQuery("SELECT customerId FROM customer LIMIT 4")) {
            while (rs.next()) validCustomerIds.add(rs.getInt(1));
        }

        while (validCustomerIds.size() < 4) validCustomerIds.add(1);

        String resSql = "INSERT INTO table_reservations (tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String billSql = "INSERT INTO bills (reservationId, totalAmount, totalAmountAfterDiscount, isPaid) VALUES (?, ?, ?, ?)";

        try (PreparedStatement psRes = con.prepareStatement(resSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBill = con.prepareStatement(billSql)) {

            for (int i = 0; i < 4; i++) {
                try {
                    con.createStatement().executeUpdate("UPDATE restaurant_tables SET status = 'occupied' WHERE tableId = " + tables[i]);
                } catch (SQLException e) { /* Ignore update error */ }

                int minutesOffset = random.nextInt(121) - 60; 
                LocalDateTime randomTime = now.plusMinutes(minutesOffset);

                psRes.setInt(1, tables[i]); 
                psRes.setInt(2, 2); 
                psRes.setInt(3, 9000 + i); 
                psRes.setInt(4, validCustomerIds.get(i)); 
                psRes.setTimestamp(5, Timestamp.valueOf(randomTime)); 
                psRes.setTimestamp(6, Timestamp.valueOf(now)); 
                psRes.setString(7, "arrived");
                
                try {
                    psRes.executeUpdate();

                    ResultSet rs = psRes.getGeneratedKeys();
                    if (rs.next()) {
                        int reservationId = rs.getInt(1);
                        psBill.setInt(1, reservationId);
                        psBill.setDouble(2, 250.0);
                        psBill.setDouble(3, 250.0);
                        psBill.setBoolean(4, false);
                        psBill.executeUpdate();
                    }
                } catch (SQLException e) {
                     System.out.println("Caught overlap in LiveDining: " + e.getMessage() + ". Skipping.");
                }
            }
        }
        System.out.println("Created exactly 4 'arrived' reservations for existing customers.");
    }

    // פונקציה חכמה שמגרילה שעה בהתאם לשעות הפתיחה והחוק של "שעתיים לפני סגירה"
    private static LocalTime getRandomOpenTime(Connection con, LocalDate date) {
        LocalTime openTime = LocalTime.of(8, 0);   
        LocalTime closeTime = LocalTime.of(23, 0); 

        java.time.DayOfWeek day = date.getDayOfWeek();
        
        // 1. בדיקת ימים מיוחדים
        if (date.equals(LocalDate.of(2026, 1, 15)) || date.equals(LocalDate.of(2026, 4, 1))) {
            closeTime = LocalTime.of(14, 0); 
        } else if (date.equals(LocalDate.of(2026, 1, 20))) {
            openTime = LocalTime.of(19, 0);
            closeTime = LocalTime.of(23, 59);
        } else if (date.equals(LocalDate.of(2026, 1, 25))) {
            openTime = LocalTime.of(16, 0);
            closeTime = LocalTime.of(23, 0);
        } 
        // 2. בדיקת ימי שגרה
        else if (day == java.time.DayOfWeek.FRIDAY) {
            closeTime = LocalTime.of(14, 0);
        } else if (day == java.time.DayOfWeek.SATURDAY) {
            openTime = LocalTime.of(20, 0);
            closeTime = LocalTime.of(23, 59); 
        } else if (day == java.time.DayOfWeek.TUESDAY) {
            if (random.nextBoolean()) {
                closeTime = LocalTime.of(12, 0); 
            } else {
                openTime = LocalTime.of(16, 0); 
                closeTime = LocalTime.of(23, 30);
            }
        }

        LocalTime maxStartTime = closeTime.minusHours(2);

        if (openTime.isAfter(maxStartTime)) return null;

        long minutesRange = ChronoUnit.MINUTES.between(openTime, maxStartTime);
        long randomSlots = minutesRange / 15; 
        long chosenSlot = random.nextInt((int) randomSlots + 1);
        
        return openTime.plusMinutes(chosenSlot * 15);
    }

    private static boolean isDayClosed(Connection con, LocalDate date) {
        return date.equals(LocalDate.of(2025, 12, 25)) || 
               date.equals(LocalDate.of(2026, 1, 1)) || 
               date.equals(LocalDate.of(2026, 1, 28)) || 
               date.equals(LocalDate.of(2026, 4, 22));
    }

    private static void initReports(Connection con, Statement stmt) throws SQLException {
        System.out.println("Initializing historical reports for November...");

        String sqlTimeMgr = "INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES ('2025-11-01', '2025-11-30', 'monthly', 'time')";
        int timeReportId = -1;
        
        try (PreparedStatement ps = con.prepareStatement(sqlTimeMgr, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) timeReportId = rs.getInt(1);
        }

        String sqlSubMgr = "INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES ('2025-11-01', '2025-11-30', 'monthly', 'subscriber')";
        int subReportId = -1;

        try (PreparedStatement ps = con.prepareStatement(sqlSubMgr, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) subReportId = rs.getInt(1);
        }

        if (timeReportId != -1 && subReportId != -1) {
            String insertTimeDetail = "INSERT INTO time_report (reportId, reportDate, avgArrival, avgLeaving) VALUES (?, ?, ?, ?)";
            String insertSubDetail = "INSERT INTO subscriber_report (reportId, reportDate, totalReservations, totalWaiting) VALUES (?, ?, ?, ?)";

            try (PreparedStatement psTime = con.prepareStatement(insertTimeDetail);
                 PreparedStatement psSub = con.prepareStatement(insertSubDetail)) {

                LocalDate start = LocalDate.of(2025, 11, 1);
                LocalDate end = LocalDate.of(2025, 11, 30);

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    psTime.setInt(1, timeReportId);
                    psTime.setDate(2, java.sql.Date.valueOf(date));
                    psTime.setInt(3, random.nextInt(30)); 
                    psTime.setInt(4, 60 + random.nextInt(90)); 
                    psTime.executeUpdate();

                    psSub.setInt(1, subReportId);
                    psSub.setDate(2, java.sql.Date.valueOf(date));
                    psSub.setInt(3, 10 + random.nextInt(20)); 
                    psSub.setInt(4, random.nextInt(5));       
                    psSub.executeUpdate();
                }
            }
        }
        System.out.println("November reports initialized successfully.");
    }
}