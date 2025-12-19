package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import data.Customer;
import data.OpeningHoursPerDay;
import data.Subscriber;
import data.TableReservation;
import data.TimeSlot;

public class DataBaseController {
	
	// START OF API:
	
	// this are the public methods that the
	// controllers can call to get OR set data.
	
	// 1. getAllReservationsQueryByCustomerId(int customerId) : ArrayList<ArrayList<Object>>
	// 2. getAllReservationsQueryByDay(LocalDate day) : ArrayList<ArrayList<Object>>
	// 3. getOpeningHoursByDate(OpeningHoursPerDay openingHours) : OpeningHoursPerDay
	// 4. getAllTablesInRestaurant() : ArrayList<ArrayList<Object>>
	// 5. checkLoginDetails(Subscriber sub) : Subscriber
	// 6. createNewReservation(TableReservation res) : boolean
	// 7. checkIfConfCodeExistsInDB(int code) : boolean
	// 8. getCustomerId(Customer cust) : int
		
	// END OF API.
	
	private static DataBaseController instance;

	// DB connection settings data
	private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
	private static String dbPassword;
	private static final String USER = "root";

	// settings data for the "connection pool"
	private static final int MAX_POOL_SIZE = 10;// Max num of connections to the DB in the "connection pool"
	private static final long MAX_IDLE_TIME = 5000;// Max time a connection can "sit in the connection pool" without
													// being used
	private static final long CHECK_INTERVAL = 2;// How often does the "pool cleaner" run and delete unused connections
													// in the "connection pool"?
	private final int NO_TABLE_FOUND = -1;// When no suitable table is found in the database.

	private BlockingQueue<PooledConnection> connectionPool;// The "connection pool"
	private ScheduledExecutorService cleanerService;// The "Pool Cleaner"

	/**
	 * Private constructor to prevent instantiation from outside. Initializes the
	 * connection pool and starts the cleanup timer.
	 */
	private DataBaseController() {
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
	 * 
	 * @return The singleton instance of DataBaseController.
	 */
	public static DataBaseController getInstance() {
		return instance;
	}

	/*
	 * /////////////////////////////////////////////////////////////////////////////
	 * /////////////////////////////// Managing the "connection pool"
	 * /////////////////////////////////////////////////////////////////////////////
	 * ///////////////////////////////////
	 */

	/**
	 * Returns a connection from the pool or creates a new one if the pool is empty.
	 *
	 * @return A PooledConnection object.
	 */
	private PooledConnection getConnection() {
		PooledConnection pConn = connectionPool.poll();// Try to get a connection from the pool

		if (pConn == null) {
			return createNewPhysicalConnection();// If there are no available connections in the pool, create a new one
		}

		pConn.touch();// Update the last used time
		return pConn;
	}

	/**
	 * * Releases a connection back to the pool or closes it if the pool is full.
	 *
	 * @param pConn The PooledConnection to be released back to the pool.
	 * @return void
	 */
	private void releaseConnection(PooledConnection pConn) {
		if (pConn != null) {
			pConn.touch();// Update the last used time
			boolean added = connectionPool.offer(pConn);// Try to add the connection back to the pool

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
			Class.forName("com.mysql.cj.jdbc.Driver");// Load the MySQL JDBC driver
			Connection conn = DriverManager.getConnection(DB_URL, USER, dbPassword);// Create a new connection

			return new PooledConnection(conn);// Wrap the connection in a PooledConnection and return it
		} catch (Exception e) {
			System.out.println("Error creating new connection: " + e.getMessage());
			return null;
		}
	}

	/**
	 * * Starts a timer that periodically checks for idle connections in the pool
	 * and closes them if they have been idle for too long.
	 * 
	 * @return void
	 */
	private void startCleanupTimer() {
		cleanerService = Executors.newSingleThreadScheduledExecutor();// Create a single-threaded scheduled executor
		cleanerService.scheduleAtFixedRate(() -> {
			if (connectionPool.isEmpty())
				return;

			List<PooledConnection> activeConnections = new ArrayList<>();// Temporary list to hold active connections
			connectionPool.drainTo(activeConnections);// Remove all connections from the pool

			long now = System.currentTimeMillis();
			for (PooledConnection pConn : activeConnections)// Loop through all connections
			{// Check if the connection has been idle for too long

				if (now - pConn.getLastUsed() > MAX_IDLE_TIME) {
					pConn.closePhysicalConnection();
				} else {
					connectionPool.offer(pConn);
				}
			}
		}, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
	}

	/*
	 * /////////////////////////////////////////////////////////////////////////////
	 * /////////////////////////////// The system's queries in the DB
	 * /////////////////////////////////////////////////////////////////////////////
	 * ///////////////////////////////////
	 */

	// הושלם
	public ArrayList<ArrayList<Object>> getAllReservationsQueryByCustomerId(int customerId) {

		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();

		// FIX 1: Retrieve the connection from the pool properly
		PooledConnection pConn = this.getConnection();

		// FIX 2: Check if the pool actually gave us a connection
		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection(); // Get the physical connection
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT * FROM table_reservations WHERE customerId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, customerId);
			rs = ps.executeQuery();

			while (rs.next()) {
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
			// FIX 3: Ensure the connection goes back to the pool
			releaseConnection(pConn);
		}
		return allReservations;
	}

	// c1
	public ArrayList<ArrayList<Object>> getAllReservationsQueryByDay(LocalDate day) {

		ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();

		// FIX 1: Use your internal method 'getConnection()' to get a real connection
		// from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check: if the pool failed to give a connection
		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// FIX 2: Use DATE() in SQL to ignore the time component (hours/minutes)
			String query = "SELECT * FROM table_reservations WHERE DATE(reservationDate) = ?";

			ps = conn.prepareStatement(query);

			// FIX 3: Convert LocalDate directly to java.sql.Date
			ps.setDate(1, java.sql.Date.valueOf(day));

			rs = ps.executeQuery();

			while (rs.next()) {
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
			// Release the connection back to the pool
			releaseConnection(pConn);
		}
		return allReservations;
	}

	// c2

	public OpeningHoursPerDay getOpeningHoursByDate(OpeningHoursPerDay openingHours) {

	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return null;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    // Create a list to hold the found slots
	    ArrayList<TimeSlot> foundSlots = new ArrayList<>();

	    try {
	        // ---------------------------------------------------------
	        // STEP 1: Check for Special Hours (Priority)
	        // ---------------------------------------------------------
	        String querySpecial = "SELECT openingTime, closingTime FROM special_hours WHERE specificDate = ?";
	        ps = conn.prepareStatement(querySpecial);
	        ps.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
	        rs = ps.executeQuery();

	        while (rs.next()) {
	            java.sql.Time sqlOpen = rs.getTime("openingTime");
	            java.sql.Time sqlClose = rs.getTime("closingTime");

	            if (sqlOpen != null && sqlClose != null) {
	                foundSlots.add(new TimeSlot(sqlOpen.toLocalTime(), sqlClose.toLocalTime()));
	            }
	        }
	        
	        // Close resources from the first query to prepare for the second (if needed)
	        rs.close();
	        ps.close();

	        // ---------------------------------------------------------
	        // STEP 2: If no special hours found, check Weekly Hours
	        // ---------------------------------------------------------
	        if (foundSlots.isEmpty()) {
	            String queryWeekly = "SELECT openingTime, closingTime FROM weekly_hours WHERE dayOfWeek = ?";
	            ps = conn.prepareStatement(queryWeekly);

	            // Since DB is now uppercase ENUM ('SUNDAY'), we can just use Java's default toString()
	            // Example: LocalDate.of(2025, 12, 21) -> getDayOfWeek() -> SUNDAY
	            ps.setString(1, openingHours.getDay().getDayOfWeek().toString());

	            rs = ps.executeQuery();

	            while (rs.next()) {
	                java.sql.Time sqlOpen = rs.getTime("openingTime");
	                java.sql.Time sqlClose = rs.getTime("closingTime");

	                if (sqlOpen != null && sqlClose != null) {
	                    foundSlots.add(new TimeSlot(sqlOpen.toLocalTime(), sqlClose.toLocalTime()));
	                }
	            }
	        }

	        // Update the original object with the list of slots we found
	        openingHours.setSlots(foundSlots);

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return openingHours;
	}

	// c3

	public ArrayList<ArrayList<Object>> getAllTablesInRestaurant() {

		ArrayList<ArrayList<Object>> allTables = new ArrayList<>();

		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT * FROM restaurant_tables";

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				ArrayList<Object> table = new ArrayList<>();
				table.add(rs.getInt("tableId"));
				table.add(rs.getInt("seatsNumber"));
				table.add(rs.getString("location")); // ENUM comes back as String
				table.add(rs.getString("status")); // ENUM comes back as String

				allTables.add(table);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}
		return allTables;
	}

	/*
	 * public boolean updateReservationDetailsQuery(TableReservation t) {
	 * PooledConnection pConn = null;//The connection from the "connection pool"
	 * PreparedStatement ps = null;//The SQL statement to be executed
	 * 
	 * try { pConn = getConnection();//Get a connection from the "connection pool"
	 * if (pConn == null) return false;
	 * 
	 * //SQL query to update the reservation details String sql =
	 * "UPDATE tablereservations SET ReservationDate = ?, numberOfDiners = ? WHERE reservationID = ?"
	 * ;
	 * 
	 * //Set the parameters for the SQL query ps =
	 * pConn.getConnection().prepareStatement(sql); ps.setTimestamp(1,
	 * t.getReservationDate()); ps.setInt(2, t.getNumberOfDiners()); ps.setInt(3,
	 * t.getReservationId());
	 * 
	 * return ps.executeUpdate() > 0;
	 * 
	 * } catch (SQLException e) { e.printStackTrace(); return false; } finally { if
	 * (ps != null) try { ps.close(); } catch (SQLException e) {}
	 * releaseConnection(pConn); } }
	 */

	/**
	 * Checks the login details of a subscriber against the database.
	 * 
	 * @param sub The Subscriber object containing the username and password to
	 *            check.
	 * @return The Subscriber object with full details if credentials are correct,
	 *         null otherwise.
	 */
	// הושלם
	public Subscriber checkLoginDetails(Subscriber sub) {
		// FIX 1: Retrieve the connection from the pool properly
		PooledConnection pConn = this.getConnection();

		// FIX 2: Check if the pool actually gave us a connection
		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection(); // Get the physical connection
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Selects the user matching both username and password
			String query = "SELECT s.subscriberId, s.customerId, s.firstName, s.lastName, s.type, "
					+ "s.personalInfo, s.username, s.password, c.phoneNumber, c.email " + "FROM subscriber s "
					+ "JOIN customer c ON s.customerId = c.customerId " + "WHERE s.username = ? AND s.password = ?";

			ps = conn.prepareStatement(query);
			ps.setString(1, sub.getUsername());
			ps.setString(2, sub.getPassword());

			rs = ps.executeQuery();

			if (rs.next()) {
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
			// FIX 3: Release connection back to pool
			releaseConnection(pConn);
		}

		return null; // Credentials are incorrect
	}

	/**
	 * Checks the availability of tables for a given number of diners and
	 * reservation time.
	 * 
	 * @param numberOfDiners  The number of diners for the reservation.
	 * @param reservationTime The desired reservation time.
	 * @return A list of available time slots as Timestamps.
	 */
	// הושלם
//	public ArrayList<Timestamp> checkingTableAvailability(int numberOfDiners, Timestamp reservationTime) {
//		ArrayList<Timestamp> availableSlots = new ArrayList<>();
//		PooledConnection pConn = null;
//		Connection conn = null; // משתנה ל-Connection הפיזי
//
//		PreparedStatement psHours = null;
//		PreparedStatement psAvailability = null;
//		ResultSet rs = null;
//
//		try {
//			// *תיקון קריטי: חילוץ ה-Connection פעם אחת*
//			conn = pConn.getConnection();
//			if (conn == null)
//				return availableSlots;
//
//			// ---------------------------------------------
//			// שלב 1: שליפת שעות הפתיחה והסגירה ליום המבוקש
//			// ---------------------------------------------
//			String hoursQuery = "SELECT openingTime, closingTime FROM restaurant_hours WHERE operatingDate = ?";
//			// שימוש ב-conn במקום pConn.getConnection()
//			psHours = conn.prepareStatement(hoursQuery);
//
//			// יצירת java.sql.Date מחלק התאריך של ה-Timestamp
//			psHours.setDate(1, new java.sql.Date(reservationTime.getTime()));
//
//			ResultSet rsHours = psHours.executeQuery();
//
//			if (!rsHours.next()) {
//				// המסעדה סגורה או שאין נתונים ליום זה
//				return availableSlots;
//			}
//
//			Time openTime = rsHours.getTime("openingTime");
//			Time closeTime = rsHours.getTime("closingTime");
//
//			// סגירת ה-Statement של השעות
//			psHours.close();
//
//			// חילוץ השעות כ-int ללולאה
//			Calendar openCal = Calendar.getInstance();
//			openCal.setTime(openTime);
//			int startHour = openCal.get(Calendar.HOUR_OF_DAY);
//
//			Calendar closeCal = Calendar.getInstance();
//			closeCal.setTime(closeTime);
//			int endHour = closeCal.get(Calendar.HOUR_OF_DAY);
//
//			// הבדיקה המתוקנת לחצות (endHour == 0)
//			if (endHour == 0 && closeCal.get(Calendar.MINUTE) == 0 && closeCal.get(Calendar.SECOND) == 0) {
//				endHour = 24;
//			}
//
//			// ---------------------------------------------
//			// שלב 2: לולאה ובדיקת זמינות לכל שעה אפשרית
//			// ---------------------------------------------
//
//			String availabilityQuery = "SELECT count(*) FROM restaurant_tables t " + "WHERE t.seatsNumber >= ? "
//					+ "AND t.tableId NOT IN ( " + "    SELECT r.tableId FROM table_reservations r "
//					+ "    WHERE r.status = 'active' " + "    AND r.reservationDate < ? " + // r.Start < New.End
//					"    AND r.leavingTime > ? " + // r.End > New.Start
//					")";
//
//			// שימוש ב-conn במקום pConn.getConnection()
//			psAvailability = conn.prepareStatement(availabilityQuery);
//
//			Calendar cal = Calendar.getInstance();
//			cal.setTimeInMillis(reservationTime.getTime());
//
//			// איפוס לדקות, שניות ומילי-שניות של התאריך המבוקש
//			cal.set(Calendar.MINUTE, 0);
//			cal.set(Calendar.SECOND, 0);
//			cal.set(Calendar.MILLISECOND, 0);
//
//			for (int hour = startHour; hour < endHour; hour++) {
//
//				cal.set(Calendar.HOUR_OF_DAY, hour);
//
//				Timestamp checkStartTime = new Timestamp(cal.getTimeInMillis());
//
//				// חישוב זמן סיום משוער: שעה אחת בלבד
//				Calendar endCal = (Calendar) cal.clone();
//				endCal.add(Calendar.HOUR_OF_DAY, 1);
//				Timestamp checkEndTime = new Timestamp(endCal.getTimeInMillis());
//
//				// מציבים פרמטרים לבדיקת הזמינות
//				psAvailability.setInt(1, numberOfDiners);
//				psAvailability.setTimestamp(2, checkEndTime);
//				psAvailability.setTimestamp(3, checkStartTime);
//
//				rs = psAvailability.executeQuery();
//
//				if (rs.next() && rs.getInt(1) > 0) {
//					// נמצא לפחות שולחן אחד פנוי
//					availableSlots.add(checkStartTime);
//				}
//				rs.close(); // סוגרים את ה-ResultSet לכל איטרציה
//			}
//
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} finally {
//			// סגירת משאבים מסודרת
//			closeResources(psAvailability, rs);
//			releaseConnection(pConn); // pConn משחרר את conn
//		}
//
//		return availableSlots;
//	}

	/**
	 * Inserts a new reservation record into the database.
	 * 
	 * @param t The TableReservation object containing all reservation details.
	 * @return true if the reservation was successfully saved to the database, false
	 *         otherwise.
	 */
	// הושלם
	public boolean createNewReservation(TableReservation res) {
		// FIX 1: Get the pooled connection properly
		PooledConnection pConn = this.getConnection();

		// FIX 2: Check if pool returned null
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection(); // Get physical connection
		PreparedStatement ps = null;

		try {
			String query = "INSERT INTO table_reservations "
					+ "(tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

			ps = conn.prepareStatement(query);
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
			releaseConnection(pConn); // Release back to pool
		}
		return false;
	}

	/**
	 * Checks if a specific confirmation code already exists in the database. Used
	 * to ensure that generated confirmation codes are unique.
	 * 
	 * @param code The confirmation code to check.
	 * @return true if the code already exists, false if it is unique.
	 */
	// הושלם
	public boolean checkIfConfCodeExistsInDB(int code) {
		// FIX 1: Get the pooled connection properly
		PooledConnection pConn = this.getConnection();

		// FIX 2: Check if pool returned null
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection(); // Get physical connection
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT 1 FROM table_reservations WHERE confirmationCode = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, code);

			rs = ps.executeQuery();

			if (rs.next()) {
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
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
	// הושלם
	public int getCustomerId(Customer cust) {
		// FIX 1: Get the pooled connection properly
		PooledConnection pConn = this.getConnection();

		// FIX 2: Check if pool returned null
		if (pConn == null) {
			return -1;
		}

		Connection conn = pConn.getConnection(); // Get physical connection
		PreparedStatement psSelect = null;
		PreparedStatement psInsert = null;
		ResultSet rs = null;
		int customerId = -1;

		try {
			// Step 1: Check if customer already exists
			String selectQuery = "SELECT customerId FROM customer WHERE phoneNumber = ? OR email = ?";

			psSelect = conn.prepareStatement(selectQuery);
			psSelect.setString(1, cust.getPhoneNumber());
			psSelect.setString(2, cust.getEmail());

			rs = psSelect.executeQuery();

			if (rs.next()) {
				return rs.getInt("customerId"); // Customer found, return ID
			}

			// Step 2: Customer doesn't exist, insert new record
			String insertQuery = "INSERT INTO customer (phoneNumber, email) VALUES (?, ?)";

			psInsert = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
			psInsert.setString(1, cust.getPhoneNumber());
			psInsert.setString(2, cust.getEmail());

			int rowsAffected = psInsert.executeUpdate();

			if (rowsAffected > 0) {
				ResultSet generatedKeys = psInsert.getGeneratedKeys();
				if (generatedKeys.next()) {
					customerId = generatedKeys.getInt(1); // Get the auto-generated ID
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			// Close resources safely
			closeResources(psSelect, rs);
			closeResources(psInsert, null);

			// FIX 3: Release connection back to pool
			releaseConnection(pConn);
		}

		return customerId;
	}

	private static void closeResources(PreparedStatement stmt, ResultSet rs) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
		}
		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException e) {
		}
	}

//	public static void main() {
//		// Create a timestamp for 20th Dec 2025 (Time doesn't matter, can be 00:00:00)
//		String strDate = "2025-12-20 00:00:00";
//		Timestamp dateToCheck = Timestamp.valueOf(strDate);
//
//		// Call your function
//		ArrayList<ArrayList<Object>> result = DataBaseController.getInstance().getAllReservationsQueryByDate(dateToCheck);
//
//		// Print size - Should be 5
//		System.out.println("Found: " + result.size());
//	}
}