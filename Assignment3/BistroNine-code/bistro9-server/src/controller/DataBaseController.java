package controller;


import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import data.Customer;
import data.Subscriber;
import data.TableReservation;

public class DataBaseController {

    private static DataBaseController instance;
    
    // DB connection settings data
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static String dbPassword; 
    private static final String USER = "root";

    // settings data for the "connection pool" 
    private static final int MAX_POOL_SIZE = 10;//Max num of connections to the DB in the "connection pool"
    private static final long MAX_IDLE_TIME = 5000;//Max time a connection can "sit in the connection pool" without being used
    private static final long CHECK_INTERVAL = 2;//How often does the "pool cleaner" run and delete unused connections in the "connection pool"?
    private final int NO_TABLE_FOUND = -1;//When no suitable table is found in the database.
    
    
    private BlockingQueue<PooledConnection> connectionPool;//The "connection pool"
    private ScheduledExecutorService cleanerService;//The "Pool Cleaner"

	/**
	 * Private constructor to prevent instantiation from outside. Initializes the
	 * connection pool and starts the cleanup timer.
	 */
    private DataBaseController() 
    {
        connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        startCleanupTimer();
        System.out.println("DataBaseController & Connection Pool Initialized.");
    }

	/**
	 * * Initializes the DataBaseController singleton with the provided password.
	 * This method must be called before getInstance().
	 *
	 * @param password The password for the database connection.
	 */
    public static void initiateDBC(String password) {
        dbPassword = password;
        if (instance == null) {
            instance = new DataBaseController();
        }
    }
    
    /**
	 * Retrieves the singleton instance of DataBaseController.
	 *@return The singleton instance of DataBaseController.
	 */
    public static DataBaseController getInstance() 
    {
        return instance;
    }

    
	/*////////////////////////////////////////////////////////////////////////////////////////////////////////////
    Managing the "connection pool"
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////*/
    
    
    /**
	 * Returns a connection from the pool or creates a new one if the pool is empty.
	 *
	 * @return A PooledConnection object.
	 */
    private PooledConnection getConnection() 
    {
        PooledConnection pConn = connectionPool.poll();//Try to get a connection from the pool
        
        if (pConn == null) {
            return createNewPhysicalConnection();//If there are no available connections in the pool, create a new one
        }
        
        pConn.touch();//Update the last used time
        return pConn;
    }
    
    /**
	 * * Releases a connection back to the pool or closes it if the pool is full.
	 *
	 * @param pConn The PooledConnection to be released back to the pool.
	 * @return void
	 */
    private void releaseConnection(PooledConnection pConn) 
    {
        if (pConn != null) {
            pConn.touch();//Update the last used time
            boolean added = connectionPool.offer(pConn);//Try to add the connection back to the pool
            
            if (!added) {
                pConn.closePhysicalConnection();
            }
        }
    }

    /**
  	 * * Creates a new physical database connection wrapped in a PooledConnection.
  	 *
  	 * 
  	 * @return A new PooledConnection object.
  	 */
    private PooledConnection createNewPhysicalConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");//Load the MySQL JDBC driver
            Connection conn = DriverManager.getConnection(DB_URL, USER, dbPassword);//Create a new connection
            
            return new PooledConnection(conn);//Wrap the connection in a PooledConnection and return it
        } catch (Exception e) {
            System.out.println("Error creating new connection: " + e.getMessage());
            return null;
        }
    }

    /**
	 * * Starts a timer that periodically checks for idle connections in the pool and closes them if they have been idle for too long.
	 *  @return void
	 */
    private void startCleanupTimer() 
    {
        cleanerService = Executors.newSingleThreadScheduledExecutor();//Create a single-threaded scheduled executor
        cleanerService.scheduleAtFixedRate(() -> 
        {
            if (connectionPool.isEmpty()) return;
            
            List<PooledConnection> activeConnections = new ArrayList<>();//Temporary list to hold active connections
            connectionPool.drainTo(activeConnections);//Remove all connections from the pool
            
            long now = System.currentTimeMillis();
            for (PooledConnection pConn : activeConnections)//Loop through all connections 
            {//Check if the connection has been idle for too long
                
                if (now - pConn.getLastUsed() > MAX_IDLE_TIME) {
                    pConn.closePhysicalConnection();
                } else {
                    connectionPool.offer(pConn);
                }
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    /*////////////////////////////////////////////////////////////////////////////////////////////////////////////
    The system's queries in the DB
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////*/

    
    //הושלם
    public ArrayList<ArrayList<Object>> getAllReservationsQuery(int customerId) 
    {
    	
    	ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();
        
        PooledConnection pConn = null;//The connection from the "connection pool"
        PreparedStatement ps = null;//The SQL statement to be executed
        ResultSet rs = null;//The result set of the executed query

        try {
            pConn = getConnection();//Get a connection from the "connection pool"
            if (pConn == null) 
            {
            	return null;
            }

            String query = "SELECT * FROM table_reservations WHERE customerId = ?";//The SQL query to be executed
            
            ps = pConn.getConnection().prepareStatement(query);
            ps.setInt(1, customerId);
            rs = ps.executeQuery();
            
            
            while (rs.next()) 
            {//Loop through the result set and build the reservation strings
            	ArrayList<Object> reservation = new ArrayList<>();
            	reservation.add(rs.getInt("reservationID"));
                reservation.add(rs.getInt("tableId")); 
                reservation.add(rs.getInt("numberOfDiners"));
                reservation.add(rs.getInt("confirmationCode"));
                reservation.add(rs.getInt("customerId"));
                reservation.add(rs.getTimestamp("reservationDate"));
                reservation.add(rs.getTimestamp("dateOfMakeReservation"));
                reservation.add(rs.getTimestamp("arrivalTime")); 
                reservation.add(rs.getTimestamp("leavingTime")); 
                reservation.add(rs.getString("status")); 
            
            	allReservations.add(reservation);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeResources(ps, rs);
            releaseConnection(pConn); 
        }
        return allReservations;
    }

    /*public boolean updateReservationDetailsQuery(TableReservation t) {
        PooledConnection pConn = null;//The connection from the "connection pool"
        PreparedStatement ps = null;//The SQL statement to be executed

        try {
            pConn = getConnection();//Get a connection from the "connection pool"
            if (pConn == null) return false;
            
            //SQL query to update the reservation details
            String sql = "UPDATE tablereservations SET ReservationDate = ?, numberOfDiners = ? WHERE reservationID = ?";
            
          //Set the parameters for the SQL query
            ps = pConn.getConnection().prepareStatement(sql);
            ps.setTimestamp(1, t.getReservationDate());
            ps.setInt(2, t.getNumberOfDiners());
            ps.setInt(3, t.getReservationId());
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException e) {}
            releaseConnection(pConn);
        }
    }*/
    
	/**
	 * Checks the login details of a subscriber against the database.
	 * 
	 * @param sub The Subscriber object containing the username and password to
	 *            check.
	 * @return The Subscriber object with full details if credentials are correct,
	 *         null otherwise.
	 */
    //הושלם
    public Subscriber checkLoginDetails(Subscriber sub) {
        PooledConnection pConn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
                
        try {
            pConn = getConnection();
            if (pConn == null) 
            { 
        		return null;
            }
            
            // Selects the user matching both username and password
            String query = "SELECT s.subscriberId, s.customerId, s.firstName, s.lastName, s.type, " +
                    "s.personalInfo, s.username, s.password, c.phoneNumber, c.email " +
                    "FROM subscriber s " +
                    "JOIN customer c ON s.customerId = c.customerId " +
                    "WHERE s.username = ? AND s.password = ?";
            
            
            ps = pConn.getConnection().prepareStatement(query);
            ps.setString(1, sub.getUsername()); 
            ps.setString(2, sub.getPassword());
            
            rs = ps.executeQuery();
            
            if (rs.next()) 
            {
            	sub.setSubscriberId(rs.getInt("subscriberId"));
            	sub.setCustomerId(rs.getInt("customerId"));
            	sub.setFirstName(rs.getString("firstName"));
                sub.setLastName(rs.getString("lastName"));
                sub.setType(rs.getString("type"));
                sub.setPersonalInfo(rs.getString("personalInfo"));
                sub.setUsername(rs.getString("username"));
                sub.setPassword(rs.getString("password"));
                sub.setPhoneNumber(rs.getString("phoneNumber"));
                sub.setEmail(rs.getString("email"));
                return sub; // Credentials are correct
            }    
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeResources(ps, rs);
            releaseConnection(pConn);
        }
        
        
        return null;  // Credentials are incorrect  
    } 
    
	/**
	 * Checks the availability of tables for a given number of diners and
	 * reservation time.
	 * 
	 * @param numberOfDiners  The number of diners for the reservation.
	 * @param reservationTime The desired reservation time.
	 * @return A list of available time slots as Timestamps.
	 */
    //הושלם
    public ArrayList<Timestamp> checkingTableAvailability(int numberOfDiners, Timestamp reservationTime) {
    	ArrayList<Timestamp> availableSlots = new ArrayList<>();
        PooledConnection pConn = null;
        Connection conn = null; // משתנה ל-Connection הפיזי
        
        PreparedStatement psHours = null;
        PreparedStatement psAvailability = null;
        ResultSet rs = null;

        try {
            pConn = getConnection();
            if (pConn == null) return availableSlots;
            
            // *תיקון קריטי: חילוץ ה-Connection פעם אחת*
            conn = pConn.getConnection(); 

            // ---------------------------------------------
            // שלב 1: שליפת שעות הפתיחה והסגירה ליום המבוקש
            // ---------------------------------------------
            String hoursQuery = "SELECT openingTime, closingTime FROM restaurant_hours WHERE operatingDate = ?";
            // שימוש ב-conn במקום pConn.getConnection()
            psHours = conn.prepareStatement(hoursQuery); 
            
            // יצירת java.sql.Date מחלק התאריך של ה-Timestamp
            psHours.setDate(1, new java.sql.Date(reservationTime.getTime())); 

            ResultSet rsHours = psHours.executeQuery();

            if (!rsHours.next()) {
                // המסעדה סגורה או שאין נתונים ליום זה
                return availableSlots; 
            }

            Time openTime = rsHours.getTime("openingTime");
            Time closeTime = rsHours.getTime("closingTime");
            
            // סגירת ה-Statement של השעות
            psHours.close();
            
            // חילוץ השעות כ-int ללולאה
            Calendar openCal = Calendar.getInstance();
            openCal.setTime(openTime);
            int startHour = openCal.get(Calendar.HOUR_OF_DAY);

            Calendar closeCal = Calendar.getInstance();
            closeCal.setTime(closeTime);
            int endHour = closeCal.get(Calendar.HOUR_OF_DAY);
            
            // הבדיקה המתוקנת לחצות (endHour == 0)
            if (endHour == 0 && closeCal.get(Calendar.MINUTE) == 0 && closeCal.get(Calendar.SECOND) == 0) {
                endHour = 24;
            }

            // ---------------------------------------------
            // שלב 2: לולאה ובדיקת זמינות לכל שעה אפשרית
            // ---------------------------------------------
            
            String availabilityQuery = "SELECT count(*) FROM restaurant_tables t " +
                    "WHERE t.seatsNumber >= ? " +
                    "AND t.tableId NOT IN ( " +
                    "    SELECT r.tableId FROM table_reservations r " +
                    "    WHERE r.status = 'active' " +
                    "    AND r.reservationDate < ? " + // r.Start < New.End
                    "    AND r.leavingTime > ? " +     // r.End > New.Start
                    ")";

            // שימוש ב-conn במקום pConn.getConnection()
            psAvailability = conn.prepareStatement(availabilityQuery);
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(reservationTime.getTime());
            
            // איפוס לדקות, שניות ומילי-שניות של התאריך המבוקש
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            for (int hour = startHour; hour < endHour; hour++) {
                
                cal.set(Calendar.HOUR_OF_DAY, hour);
                
                Timestamp checkStartTime = new Timestamp(cal.getTimeInMillis());
                
                // חישוב זמן סיום משוער: שעה אחת בלבד
                Calendar endCal = (Calendar) cal.clone();
                endCal.add(Calendar.HOUR_OF_DAY, 1); 
                Timestamp checkEndTime = new Timestamp(endCal.getTimeInMillis());

                // מציבים פרמטרים לבדיקת הזמינות
                psAvailability.setInt(1, numberOfDiners);
                psAvailability.setTimestamp(2, checkEndTime);
                psAvailability.setTimestamp(3, checkStartTime);

                rs = psAvailability.executeQuery();
                
                if (rs.next() && rs.getInt(1) > 0) {
                    // נמצא לפחות שולחן אחד פנוי
                    availableSlots.add(checkStartTime);
                }
                rs.close(); // סוגרים את ה-ResultSet לכל איטרציה
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // סגירת משאבים מסודרת
            closeResources(psAvailability, rs);
            releaseConnection(pConn); // pConn משחרר את conn
        }

        return availableSlots;
    }
            
            
            
            
    
    /**
     * Searches for a specific Table ID that meets the capacity requirements 
     * and is not already reserved at the requested time.
     * @param numberOfDiners The minimum capacity required for the table.
     * @param reservationTime The time of the reservation to check availability for.
     * @return The ID of an available table, or -1 if no suitable table is found.
     */
    //לא הושלם
    public int catchTable(int numberOfDiners, Timestamp reservationTime) {
        PooledConnection pConn = null;
        PreparedStatement psSelect = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;
        int foundTableId = -1;

        try {
            pConn = getConnection();
            if (pConn == null)
            	return -1;

            // Logic: Find a table with sufficient capacity that is NOT present in the reservations 
            // table for the exact same time.
            String query = "SELECT tableID FROM tables " +
                           "WHERE capacity >= ? " +
                           "AND tableID NOT IN " +
                           "(SELECT tableID FROM tablereservations WHERE reservationDate = ?)";

            psSelect = pConn.getConnection().prepareStatement(query);
            psSelect.setInt(1, numberOfDiners);
            psSelect.setTimestamp(2, reservationTime);

            rs = psSelect.executeQuery();

            if (rs.next()) {
                foundTableId = rs.getInt("tableID");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeResources(psSelect, rs);
            releaseConnection(pConn);
        }
        return foundTableId;
    }
    
    /**
     * Inserts a new reservation record into the database.
     * @param t The TableReservation object containing all reservation details.
     * @return true if the reservation was successfully saved to the database, false otherwise.
     */
    //הושלם
    public boolean createNewReservation(TableReservation res) {
        PooledConnection pConn = null;
        PreparedStatement ps = null;
        

        try {
            pConn = getConnection();
            if (pConn == null) return false;
            
            // Inserts the new reservation details into the database
            String query = "INSERT INTO table_reservations " +
                    "(tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            ps = pConn.getConnection().prepareStatement(query);
            ps.setInt(1, res.getTableId());
            ps.setInt(2, res.getNumberOfDiners());
            ps.setInt(3, res.getConfirmationCode()); 
            ps.setInt(4, res.getCustomerId());
            ps.setTimestamp(5, res.getReservationDate());
            ps.setTimestamp(6, res.getArrivalTime()); 
            ps.setTimestamp(7, res.getLeavingTime());

            int result = ps.executeUpdate();
            
            if (result == 1) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeResources(ps, null);
            releaseConnection(pConn);
        }
        return false;
    } 
    
    /**
     * Checks if a specific confirmation code already exists in the database.
     * Used to ensure that generated confirmation codes are unique.
     * @param code The confirmation code to check.
     * @return true if the code already exists, false if it is unique.
     */
    //הושלם
    public boolean checkIfConfCodeExistsInDB(int code) {
        PooledConnection pConn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        

        try {
            pConn = getConnection();
            if (pConn == null) return false;

            String query = "SELECT 1 FROM table_reservations WHERE confirmationCode = ?";
            
            ps = pConn.getConnection().prepareStatement(query);
            ps.setInt(1, code);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
        	closeResources(ps, rs);
            releaseConnection(pConn);
        }
        return false;
    }
    
	/**
	 * Retrieves the customer ID for a given customer based on their phone number or
	 * email. If the customer does not exist, a new record is created in the
	 * database.
	 * 
	 * @param cust The Customer object containing phone number and email.
	 * @return The customer ID if found or created, -1 on failure.
	 */
    //הושלם
    public int getCustomerId(Customer cust) {
    	PooledConnection pConn = null;
        PreparedStatement psSelect = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;
        int customerId = -1;

        try {
        	pConn = getConnection();
            if (pConn == null) return -1;
        	
         //
            String selectQuery = "SELECT customerId FROM customer WHERE phoneNumber = ? OR email = ?";

            
            psSelect = pConn.getConnection().prepareStatement(selectQuery);
            psSelect.setString(1, cust.getPhoneNumber());
            psSelect.setString(2, cust.getEmail());
            
            rs = psSelect.executeQuery();
            
            if (rs.next()) {
                
                return rs.getInt("customerId");
            }

            
            String insertQuery = "INSERT INTO customer (phoneNumber, email) VALUES (?, ?)";
            
            psInsert = pConn.getConnection().prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            psInsert.setString(1, cust.getPhoneNumber());
            psInsert.setString(2, cust.getEmail());
            
            int rowsAffected = psInsert.executeUpdate();
            
            if (rowsAffected > 0) {
                
                ResultSet generatedKeys = psInsert.getGeneratedKeys();
                if (generatedKeys.next()) {
                    customerId = generatedKeys.getInt(1); 
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            
            closeResources(psSelect, rs);
            closeResources(psInsert, null);
            releaseConnection(pConn);
        }
        
        return customerId;
    }
    
    
    
    private static void closeResources( PreparedStatement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) {}
        try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
    }
}