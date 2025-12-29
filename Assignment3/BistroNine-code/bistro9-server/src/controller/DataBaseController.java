package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import data.Bill;
import data.Customer;
import data.OpeningHoursPerDay;
import data.Subscriber;
import data.Table;
import data.TableReservation;
import data.TimeSlot;
import data.WaitList;

public class DataBaseController {

	// START OF API:

	// this are the public methods that the
	// controllers can call to get OR set data.
	//.
	// 1. getAllReservationsQueryByCustomerId(int) : ArrayList<ArrayList<Object>>
	// 2. getAllReservationsQueryByDay(LocalDate) : ArrayList<ArrayList<Object>>
	// 3. getOpeningHoursByDate(OpeningHoursPerDay) : OpeningHoursPerDay
	// 4. getAllTablesInRestaurant() : ArrayList<ArrayList<Object>>
	// 5. checkLoginDetails(Subscriber) : Subscriber
	// 6. createNewReservation(TableReservation) : boolean
	// 7. checkIfConfCodeExistsInDB(int) : boolean
	// 8. getCustomerId(Customer) : int
	// 9. deleteReservationByConfCode(int) : boolean
	// 10. updateReservationStatus(int, String) : boolean
	// 11. getBillDetails(Bill) : Bill
	// 12. addNewSubscriber(Subscriber) : int
	// 13. updateSubscriberDetails(Subscriber) : boolean
	// 14. getAllSubscribersQuery() : ArrayList<ArrayList<Object>>
	// 15. getDiscountQuery(String) : float
	// 16. createNewBillQuery(Bill) : boolean
	// 17. getReservationsByConferenceCodeQuery(TableReservation) : boolean
	// 18. getBillByReservationId(Bill) : boolean
	// 19. payBill(int, boolean, String) : int
	// 20. getReservationByReservationId(TableReservation) : boolean
	// 21. getCustomerType(int) : String
	// 22. updateOpeningTimeQuery(String, ArrayList<TimeSlot>) : boolean
	// 23. addNewSpecialOpeningTimeQuery(OpeningHoursPerDay) : boolean
	// 24. getAllReservationsQuery() : ArrayList<ArrayList<Object>>
	// 25. updateReservationLeavingTime(int, Timestamp) : boolean
	// 26. getAllReservationsActiveQuery() : ArrayList<ArrayList<Object>>
	// 27. getCustomerByCustomerId(Subscriber) : boolean
	// 28. updateTableStatus(int, String) : boolean
	// 29. updateTableSeatsNumber(int, int) : boolean
	// 30. updateReservation(TableReservation) : boolean
	// 31. addTableQuery(Table) : Table
	// 32. deleteTableQuery(int) : boolean
	// 33. getWaitingListQuery() : ArrayList<ArrayList<Object>>
	// 34. getTableByTableIdQuery(Table) : Table
	// 35. updateStatusInWaitingListQuery(WaitList) : boolean
	// 36. checkIfConfCodeExistsInWaitingList(int) : boolean
	// 37. createNewWaitQuery(WaitList) : boolean
	
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
				System.out.println("Found slot for TUESDAY: " + sqlOpen + " - " + sqlClose);
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

				// Since DB is now uppercase ENUM ('SUNDAY'), we can just use Java's default
				// toString()
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

	// d1

	/**
	 * Deletes a reservation from the database based on its unique confirmation
	 * code. * @param confirmationCode The unique confirmation code of the
	 * reservation to delete.
	 * 
	 * @return true if the reservation was found and deleted, false otherwise.
	 */
	public boolean deleteReservationByConfCode(int confirmationCode) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			String query = "DELETE FROM table_reservations WHERE confirmationCode = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, confirmationCode);

			// executeUpdate returns the number of rows affected
			int rowsAffected = ps.executeUpdate();

			// If rowsAffected > 0, it means the reservation existed and was deleted
			if (rowsAffected > 0) {
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

	// d2

	/**
	 * Updates the status of a reservation (e.g., from 'active' to 'cancelled').
	 * 
	 * @param confirmationCode The unique code of the reservation.
	 * @param newStatus        The new status string (Must match SQL ENUM: 'active',
	 *                         'arrived', 'cancelled', 'completed').
	 * @return true if updated successfully, false otherwise.
	 */
	public boolean updateReservationStatus(int confirmationCode, String newStatus) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			String query = "UPDATE table_reservations SET status = ? WHERE confirmationCode = ?";

			ps = conn.prepareStatement(query);
			ps.setString(1, newStatus);
			ps.setInt(2, confirmationCode);

			int rowsAffected = ps.executeUpdate();

			if (rowsAffected > 0) {
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

	// d3

	/**
	 * Retrieves bill details from the database based on the reservation ID and
	 * updates the provided Bill object.
	 * 
	 * @param bill The Bill object containing the reservationId.
	 * @return The updated Bill object (or null if connection failed).
	 */
	public Bill getBillDetails(Bill bill) {
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
			String query = "SELECT * FROM bills WHERE reservationId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, bill.getReservationId());

			rs = ps.executeQuery();

			if (rs.next()) {
				// Update the bill object with data from the DB
				bill.setBillId(rs.getInt("billId"));
				bill.setTotalAmount(rs.getFloat("totalAmount"));
				bill.setTotalAmountAfterDiscount(rs.getFloat("totalAmountAfterDiscount"));
				bill.setDiscountSize(rs.getFloat("discountPercentage"));
				bill.setPaid(rs.getBoolean("isPaid"));

				// Handle the ENUM: if null, it stays null; otherwise get the string
				String method = rs.getString("paymentMethod");
				if (method != null) {
					bill.setPaymentMethod(method);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return bill;
	}

	// n6
	public boolean createNewReservation(TableReservation res) {
	    // 1. Get the pooled connection
	    PooledConnection pConn = this.getConnection();

	    // 2. Check if pool returned null
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement psInsert = null;
	    PreparedStatement psSelect = null;
	    ResultSet rsKeys = null;
	    ResultSet rsData = null;

	    try {
	        // STEP A: Insert the reservation
	        String insertQuery = "INSERT INTO table_reservations "
	                + "(tableId, numberOfDiners, confirmationCode, customerId, reservationDate, arrivalTime, leavingTime) "
	                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

	        // ADDED: Statement.RETURN_GENERATED_KEYS to get the ID back
	        psInsert = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);

	        // tableId is nullable (might be assigned later)
	        if (res.getTableId() > 0) {
	            psInsert.setInt(1, res.getTableId());
	        } else {
	            psInsert.setNull(1, java.sql.Types.INTEGER);
	        }
	        
	        psInsert.setInt(2, res.getNumberOfDiners());
	        psInsert.setInt(3, res.getConfirmationCode());
	        psInsert.setInt(4, res.getCustomerId());
	        psInsert.setTimestamp(5, res.getReservationDate());
	        psInsert.setTimestamp(6, res.getArrivalTime());
	        psInsert.setTimestamp(7, res.getLeavingTime());

	        int result = psInsert.executeUpdate();

	        if (result == 1) {
	            // STEP B: Retrieve the generated reservationId
	            rsKeys = psInsert.getGeneratedKeys();
	            if (rsKeys.next()) {
	                res.setReservationId(rsKeys.getInt(1)); // Update object reference with new ID
	            }

	            // STEP C: Retrieve the DB-generated defaults (dateOfMakeReservation, status)
	            // We need a separate SELECT because getGeneratedKeys only returns the ID.
	            String selectQuery = "SELECT dateOfMakeReservation, status FROM table_reservations WHERE reservationId = ?";
	            psSelect = conn.prepareStatement(selectQuery);
	            psSelect.setInt(1, res.getReservationId());
	            
	            rsData = psSelect.executeQuery();
	            
	            if (rsData.next()) {
	                // Update object reference with DB timestamps and defaults
	                res.setDateOfMakeReservation(rsData.getTimestamp("dateOfMakeReservation"));
	                res.setStatus(rsData.getString("status"));
	            }

	            return true;
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        // Close all resources
	        closeResources(psInsert, rsKeys);
	        closeResources(psSelect, rsData);
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

	// e1

	/**
	 * Inserts a new subscriber into the database and returns the generated
	 * subscriberId.
	 * 
	 * @param sub The Subscriber object containing details (firstName, lastName,
	 *            type, etc.) and the linked customerId.
	 * @return The new subscriberId if successful, or -1 if failed.
	 */
	public int addNewSubscriber(Subscriber sub) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return -1;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		int newSubscriberId = -1;

		try {
			String query = "INSERT INTO subscriber (customerId, firstName, lastName, type, personalInfo, username, password) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

			// We must specify Statement.RETURN_GENERATED_KEYS to get the auto-increment ID
			// back
			ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			ps.setInt(1, sub.getCustomerId());
			ps.setString(2, sub.getFirstName());
			ps.setString(3, sub.getLastName());
			ps.setString(4, sub.getType());
			ps.setString(5, sub.getPersonalInfo());
			ps.setString(6, sub.getUsername());
			ps.setString(7, sub.getPassword());

			int rowsAffected = ps.executeUpdate();

			if (rowsAffected > 0) {
				// Retrieve the generated primary key (subscriberId)
				rs = ps.getGeneratedKeys();
				if (rs.next()) {
					newSubscriberId = rs.getInt(1);
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return newSubscriberId;
	}

	// e2

	/**
	 * Updates the contact details (phone/email) of a subscriber (linked to the
	 * customer table). Only non-null fields in the Subscriber object will be
	 * updated in the database.
	 * 
	 * @param sub The Subscriber object containing the customerId and the new
	 *            phone/email.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateSubscriberDetails(Subscriber sub) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		// Flags to check what we need to update
		boolean updatePhone = (sub.getPhoneNumber() != null);
		boolean updateEmail = (sub.getEmail() != null);

		// If both are null, there is nothing to update
		if (!updatePhone && !updateEmail) {
			return false;
		}

		try {
			// Build the query dynamically based on which fields are present
			StringBuilder query = new StringBuilder("UPDATE customer SET ");

			if (updatePhone) {
				query.append("phoneNumber = ?");
			}

			if (updatePhone && updateEmail) {
				query.append(", "); // Add comma if we are updating both
			}

			if (updateEmail) {
				query.append("email = ?");
			}

			query.append(" WHERE customerId = ?");

			ps = conn.prepareStatement(query.toString());

			// Set the parameters dynamically
			int paramIndex = 1;

			if (updatePhone) {
				ps.setString(paramIndex++, sub.getPhoneNumber());
			}

			if (updateEmail) {
				ps.setString(paramIndex++, sub.getEmail());
			}

			// The last parameter is always the customerId
			ps.setInt(paramIndex, sub.getCustomerId());

			int rowsAffected = ps.executeUpdate();

			if (rowsAffected > 0) {
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace(); // This will print if you try to update to a duplicate phone/email
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn); // Release back to pool
		}

		return false;
	}

	// e3

	/**
	 * Retrieves all subscribers from the database, including their contact info
	 * from the customer table.
	 * 
	 * @return An ArrayList of rows, where each row is an ArrayList of objects.
	 */
	public ArrayList<ArrayList<Object>> getAllSubscribersQuery() {
		ArrayList<ArrayList<Object>> allSubscribers = new ArrayList<>();

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
			// We need a JOIN to get the phone number and email from the customer table
			String query = "SELECT s.subscriberId, s.customerId, s.firstName, s.lastName, "
					+ "s.type, s.personalInfo, s.username, s.password, " + "c.phoneNumber, c.email "
					+ "FROM subscriber s " + "JOIN customer c ON s.customerId = c.customerId";

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				ArrayList<Object> subscriberRow = new ArrayList<>();

				// 0. subscriberId
				subscriberRow.add(rs.getInt("subscriberId"));

				// 1. customerId
				subscriberRow.add(rs.getInt("customerId"));

				// 2. firstName
				subscriberRow.add(rs.getString("firstName"));

				// 3. lastName
				subscriberRow.add(rs.getString("lastName"));

				// 4. type
				subscriberRow.add(rs.getString("type"));

				// 5. personalInfo (can be null in DB, returns null here)
				subscriberRow.add(rs.getString("personalInfo"));

				// 6. username
				subscriberRow.add(rs.getString("username"));

				// 7. password
				subscriberRow.add(rs.getString("password"));

				// 8. phoneNumber
				subscriberRow.add(rs.getString("phoneNumber"));

				// 9. email
				subscriberRow.add(rs.getString("email"));

				allSubscribers.add(subscriberRow);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return allSubscribers;
	}
	
	/**
	 * Retrieves the discount percentage for a specific customer type.
	 * @param type The type of customer (e.g., 'subscriber', 'customer').
	 * @return The discount percentage as a float (e.g., 10.0 for 10%), or 0.0 if not found.
	 */
	public float getDiscountQuery(String type) {
	    float discount = 0.0f;

	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return 0.0f;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        String query = "SELECT discount FROM restaurant_discount WHERE type_customer = ?";

	        ps = conn.prepareStatement(query);
	        ps.setString(1, type);

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            discount = rs.getFloat("discount");
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return discount;
	}
	
	
	
	
	/**
	 * Inserts a new bill record into the database.
	 * @param bill The Bill object containing reservationId, amounts, and discount info.
	 * @return true if the bill was successfully created, false otherwise.
	 */
	public boolean createNewBillQuery(Bill bill) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "INSERT INTO bills (reservationId, totalAmount, discountPercentage, totalAmountAfterDiscount) "
	                     + "VALUES (?, ?, ?, ?)";

	        ps = conn.prepareStatement(query);
	        
	        ps.setInt(1, bill.getReservationId());
	        ps.setDouble(2, bill.getTotalAmount());
	        ps.setDouble(3, bill.getDiscountSize()); // Assuming getDiscountSize() returns the percentage
	        ps.setDouble(4, bill.getTotalAmountAfterDiscount());

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	
	//n17
	/**
	 * Retrieves reservation details using the confirmation code.
	 * Updates the passed TableReservation object with the data found.
	 * @param res The TableReservation object containing the confirmation code.
	 * @return true if the reservation was found and object updated, false otherwise.
	 */
	public boolean getReservationsByConferenceCodeQuery(TableReservation res) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        String query = "SELECT * FROM table_reservations WHERE confirmationCode = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, res.getConfirmationCode());

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            // Found the reservation, update the object
	            res.setReservationId(rs.getInt("reservationId"));
	            res.setTableId(rs.getInt("tableId"));
	            res.setNumberOfDiners(rs.getInt("numberOfDiners"));
	            res.setCustomerId(rs.getInt("customerId"));
	            res.setReservationDate(rs.getTimestamp("reservationDate"));
	            
	            // --- THIS LINE WAS MISSING ---
	            res.setDateOfMakeReservation(rs.getTimestamp("dateOfMakeReservation")); 
	            // -----------------------------

	            res.setArrivalTime(rs.getTimestamp("arrivalTime"));
	            res.setLeavingTime(rs.getTimestamp("leavingTime"));
	            res.setStatus(rs.getString("status")); 

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
	 * Retrieves bill details using the reservation ID.
	 * Updates the passed Bill object with the data found.
	 * @param bill The Bill object containing the reservationId.
	 * @return true if the bill was found and object updated, false otherwise.
	 */
	public boolean getBillByReservationId(Bill bill) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        String query = "SELECT * FROM bills WHERE reservationId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, bill.getReservationId());

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            // Found the bill, update the object
	            bill.setBillId(rs.getInt("billId"));
	            bill.setTotalAmount(rs.getDouble("totalAmount"));
	            bill.setDiscountSize(rs.getFloat("discountPercentage")); // Matches the column 'discountPercentage'
	            bill.setTotalAmountAfterDiscount(rs.getDouble("totalAmountAfterDiscount"));
	            bill.setPaid(rs.getBoolean("isPaid"));
	            
	            String method = rs.getString("paymentMethod");
	            if (method != null) {
	                bill.setPaymentMethod(method);
	            }

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
	 * Updates a bill to status 'Paid' and sets the payment method.
	 * Returns the associated reservationId upon success.
	 * @param billId The ID of the bill to pay.
	 * @param isPaid The new payment status (true).
	 * @param paymentMethod The method used ('Cash', 'Credit', 'App').
	 * @return The reservationId associated with the bill if successful, or 0 if failed.
	 */
	public int payBill(int billId, boolean isPaid, String paymentMethod) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return 0;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement psUpdate = null;
	    PreparedStatement psSelect = null;
	    ResultSet rs = null;
	    int reservationId = 0;

	    try {
	        // Step 1: Update the bill status
	        String updateQuery = "UPDATE bills SET isPaid = ?, paymentMethod = ? WHERE billId = ?";
	        psUpdate = conn.prepareStatement(updateQuery);
	        
	        psUpdate.setBoolean(1, isPaid);
	        psUpdate.setString(2, paymentMethod);
	        psUpdate.setInt(3, billId);

	        int rowsAffected = psUpdate.executeUpdate();

	        // Step 2: If update was successful, retrieve the reservationId
	        if (rowsAffected > 0) {
	            String selectQuery = "SELECT reservationId FROM bills WHERE billId = ?";
	            psSelect = conn.prepareStatement(selectQuery);
	            psSelect.setInt(1, billId);
	            
	            rs = psSelect.executeQuery();
	            
	            if (rs.next()) {
	                reservationId = rs.getInt("reservationId");
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(psUpdate, null);
	        closeResources(psSelect, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return reservationId;
	}
	
	
	/**
	 * Retrieves reservation details using the reservation ID.
	 * Updates the passed TableReservation object with the data found.
	 * @param res The TableReservation object containing the reservationId.
	 * @return true if the reservation was found and object updated, false otherwise.
	 */
	public boolean getReservationByReservationId(TableReservation res) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        String query = "SELECT * FROM table_reservations WHERE reservationId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, res.getReservationId());

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            res.setTableId(rs.getInt("tableId"));
	            res.setNumberOfDiners(rs.getInt("numberOfDiners"));
	            res.setConfirmationCode(rs.getInt("confirmationCode"));
	            res.setCustomerId(rs.getInt("customerId"));
	            res.setReservationDate(rs.getTimestamp("reservationDate"));
	            res.setArrivalTime(rs.getTimestamp("arrivalTime"));
	            res.setLeavingTime(rs.getTimestamp("leavingTime"));
	            res.setStatus(rs.getString("status"));

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
	 * Retrieves the customer type string based on the customer ID.
	 * @param customerId The ID to check.
	 * @return The type string (e.g., 'subscriber', 'customer') or null if ID not found.
	 */
	public String getCustomerType(int customerId) {
	    String type = null;

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
	        // Use LEFT JOIN to check both tables in one query
	        String query = "SELECT s.type FROM customer c " 
	                     + "LEFT JOIN subscriber s ON c.customerId = s.customerId " 
	                     + "WHERE c.customerId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, customerId);

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            String dbType = rs.getString("type");
	            
	            if (dbType != null) {
	                // User is in the subscriber table (return 'subscriber', 'restaurant manager', etc.)
	                type = dbType;
	            } else {
	                // User exists in customer table but NOT in subscriber table
	                type = "customer";
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return type;
	}
	
	
	/**
	 * Updates the opening hours for a specific day by deleting old entries 
	 * and inserting new ones (Transaction-based).
	 * @param day The day of the week (e.g., 'SUNDAY').
	 * @param openingTimes The list of new TimeSlot objects.
	 * @return true if the transaction was successful, false otherwise.
	 */
	public boolean updateOpeningTimeQuery(String day, ArrayList<TimeSlot> openingTimes) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement psDelete = null;
	    PreparedStatement psInsert = null;
	    boolean success = false;

	    try {
	        // START TRANSACTION
	        conn.setAutoCommit(false);

	        // Step 1: Delete all existing hours for this day
	        String deleteQuery = "DELETE FROM weekly_hours WHERE dayOfWeek = ?";
	        psDelete = conn.prepareStatement(deleteQuery);
	        psDelete.setString(1, day);
	        psDelete.executeUpdate();

	        // Step 2: Insert the new time slots
	        String insertQuery = "INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES (?, ?, ?)";
	        psInsert = conn.prepareStatement(insertQuery);

	        for (TimeSlot slot : openingTimes) {
	            psInsert.setString(1, day);
	            psInsert.setTime(2, java.sql.Time.valueOf(slot.getOpen()));
	            psInsert.setTime(3, java.sql.Time.valueOf(slot.getClose()));
	            
	            psInsert.executeUpdate();
	        }

	        // COMMIT TRANSACTION
	        conn.commit();
	        success = true;

	    } catch (SQLException e) {
	        e.printStackTrace();
	        try {
	            // ROLLBACK if something went wrong
	            if (conn != null) {
	                conn.rollback();
	            }
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        }
	    } finally {
	        // Restore AutoCommit to true
	        try {
	            if (conn != null) {
	                conn.setAutoCommit(true);
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        
	        closeResources(psDelete, null);
	        closeResources(psInsert, null);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return success;
	}
	
	
	/**
	 * Adds (or updates) special opening hours for a specific date.
	 * Deletes any existing entries for that date first, then inserts new ones (Transaction-based).
	 * @param openingHours The OpeningHoursPerDay object containing the date and list of slots.
	 * @return true if the transaction was successful, false otherwise.
	 */
	public boolean addNewSpecialOpeningTimeQuery(OpeningHoursPerDay openingHours) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement psDelete = null;
	    PreparedStatement psInsert = null;
	    boolean success = false;

	    try {
	        // START TRANSACTION
	        conn.setAutoCommit(false);

	        // Step 1: Delete all existing special hours for this specific date
	        String deleteQuery = "DELETE FROM special_hours WHERE specificDate = ?";
	        psDelete = conn.prepareStatement(deleteQuery);
	        // Convert LocalDate to java.sql.Date
	        psDelete.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
	        psDelete.executeUpdate();

	        // Step 2: Insert the new time slots
	        String insertQuery = "INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES (?, ?, ?)";
	        psInsert = conn.prepareStatement(insertQuery);

	        for (TimeSlot slot : openingHours.getSlots()) {
	            psInsert.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
	            psInsert.setTime(2, java.sql.Time.valueOf(slot.getOpen()));
	            psInsert.setTime(3, java.sql.Time.valueOf(slot.getClose()));
	            
	            psInsert.executeUpdate();
	        }

	        // COMMIT TRANSACTION
	        conn.commit();
	        success = true;

	    } catch (SQLException e) {
	        e.printStackTrace();
	        try {
	            // ROLLBACK if something went wrong
	            if (conn != null) {
	                conn.rollback();
	            }
	        } catch (SQLException ex) {
	            ex.printStackTrace();
	        }
	    } finally {
	        // Restore AutoCommit to true
	        try {
	            if (conn != null) {
	                conn.setAutoCommit(true);
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        
	        closeResources(psDelete, null);
	        closeResources(psInsert, null);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return success;
	}
	
	
	/**
	 * Retrieves all reservations from the database.
	 * @return An ArrayList of rows, where each row is an ArrayList of objects representing a reservation.
	 */
	public ArrayList<ArrayList<Object>> getAllReservationsQuery() {
	    ArrayList<ArrayList<Object>> allReservations = new ArrayList<>();

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
	        String query = "SELECT * FROM table_reservations";

	        ps = conn.prepareStatement(query);
	        rs = ps.executeQuery();

	        while (rs.next()) {
	            ArrayList<Object> reservation = new ArrayList<>();
	            // Note: Make sure these column names match your SQL schema exactly
	            reservation.add(rs.getInt("reservationId")); 
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
	        releaseConnection(pConn); // Release back to pool
	    }
	    
	    return allReservations;
	}
	
	
	/**
	 * Updates the leaving time for a specific reservation.
	 * @param reservationId The ID of the reservation to update.
	 * @param leavingTime The new timestamp to set as the leaving time.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateReservationLeavingTime(int reservationId, java.sql.Timestamp leavingTime) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "UPDATE table_reservations SET leavingTime = ? WHERE reservationId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setTimestamp(1, leavingTime);
	        ps.setInt(2, reservationId);

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	 * Retrieves all reservations with status 'active' or 'arrived'.
	 * @return An ArrayList of rows, where each row is an ArrayList of objects.
	 */
	public ArrayList<ArrayList<Object>> getAllReservationsActiveQuery() {
	    ArrayList<ArrayList<Object>> activeReservations = new ArrayList<>();

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
	        // Use IN clause to get both status types efficiently
	        String query = "SELECT * FROM table_reservations WHERE status IN ('active', 'arrived')";

	        ps = conn.prepareStatement(query);
	        rs = ps.executeQuery();

	        while (rs.next()) {
	            ArrayList<Object> reservation = new ArrayList<>();
	            
	            reservation.add(rs.getInt("reservationId")); 
	            reservation.add(rs.getInt("tableId"));
	            reservation.add(rs.getInt("numberOfDiners"));
	            reservation.add(rs.getInt("confirmationCode"));
	            reservation.add(rs.getInt("customerId"));
	            reservation.add(rs.getTimestamp("reservationDate"));
	            reservation.add(rs.getTimestamp("dateOfMakeReservation"));
	            reservation.add(rs.getTimestamp("arrivalTime"));
	            reservation.add(rs.getTimestamp("leavingTime"));
	            reservation.add(rs.getString("status"));

	            activeReservations.add(reservation);
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }
	    
	    return activeReservations;
	}
	
	
	/**
	 * Retrieves customer details by ID.
	 * If the customer is a subscriber, it fetches the full profile.
	 * If the customer is a regular customer, it fetches only contact info (phone/email).
	 * @param sub The Subscriber object containing the customerId to look up.
	 * @return true if the customer was found and object updated, false otherwise.
	 */
	public boolean getCustomerByCustomerId(Subscriber sub) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        // LEFT JOIN ensures we get customer info even if they are not in the subscriber table
	        String query = "SELECT c.phoneNumber, c.email, "
	                     + "s.subscriberId, s.firstName, s.lastName, s.username, s.type, s.personalInfo "
	                     + "FROM customer c "
	                     + "LEFT JOIN subscriber s ON c.customerId = s.customerId "
	                     + "WHERE c.customerId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, sub.getCustomerId());

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            // 1. Always set basic contact info (from customer table)
	            sub.setPhoneNumber(rs.getString("phoneNumber"));
	            sub.setEmail(rs.getString("email"));

	            // 2. Check if this is a subscriber (if username is not null)
	            String username = rs.getString("username");
	            
	            if (username != null) {
	                // It is a subscriber, populate the rest
	                sub.setSubscriberId(rs.getInt("subscriberId"));
	                sub.setFirstName(rs.getString("firstName"));
	                sub.setLastName(rs.getString("lastName"));
	                sub.setUsername(username);
	                sub.setType(rs.getString("type"));
	                sub.setPersonalInfo(rs.getString("personalInfo"));
	            } 
	            // If username is null, those fields remain null in the object, 
	            // which allows your controller to identify them as a regular customer.
	            
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
	 * Updates the status of a restaurant table.
	 * @param tableId The ID of the table to update.
	 * @param newStatus The new status string (Must match SQL ENUM: 'available', 'reserved', 'occupied').
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateTableStatus(int tableId, String newStatus) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "UPDATE restaurant_tables SET status = ? WHERE tableId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setString(1, newStatus);
	        ps.setInt(2, tableId);

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	 * Updates the number of seats for a restaurant table.
	 * @param tableId The ID of the table to update.
	 * @param newSeats The new number of seats.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateTableSeatsNumber(int tableId, int newSeats) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "UPDATE restaurant_tables SET seatsNumber = ? WHERE tableId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, newSeats);
	        ps.setInt(2, tableId);

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	 * Updates an existing reservation with new details (Table ID, Arrival Time, Status).
	 * @param res The TableReservation object containing the updated data.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateReservation(TableReservation res) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        // We update the fields that change during check-in
	        String query = "UPDATE table_reservations SET tableId = ?, arrivalTime = ?, status = ? WHERE reservationId = ?";

	        ps = conn.prepareStatement(query);
	        
	        // 1. Table ID (might be null if not assigned yet, but in check-in it should be set)
	        if (res.getTableId() > 0) {
	            ps.setInt(1, res.getTableId());
	        } else {
	            ps.setNull(1, java.sql.Types.INTEGER);
	        }

	        // 2. Arrival Time
	        ps.setTimestamp(2, res.getArrivalTime());

	        // 3. Status
	        ps.setString(3, res.getStatus());

	        // 4. WHERE clause
	        ps.setInt(4, res.getReservationId());

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	 * Inserts a new table into the database and retrieves the generated ID.
	 * @param table The Table object containing seatsNumber, location, and status.
	 * @return The updated Table object with the new tableId, or null if insertion failed.
	 */
	public Table addTableQuery(Table table) {
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
	        String query = "INSERT INTO restaurant_tables (seatsNumber, location, status) VALUES (?, ?, ?)";

	        // Request the generated keys (auto-increment ID)
	        ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
	        
	        ps.setInt(1, table.getSeatsNumber());
	        ps.setString(2, table.getLocation());
	        ps.setString(3, table.getStatus());

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
	            // Retrieve the generated tableId
	            rs = ps.getGeneratedKeys();
	            if (rs.next()) {
	                table.setTableId(rs.getInt(1));
	                return table; // Return the object with the new ID
	            }
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return null;
	}
	
	
	/**
	 * Deletes a table from the database.
	 * Warning: This will fail if there are existing reservations (past or future) linked to this table
	 * due to Foreign Key constraints, unless those reservations are deleted first.
	 * @param tableId The ID of the table to delete.
	 * @return true if the deletion was successful, false otherwise.
	 */
	public boolean deleteTableQuery(int tableId) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "DELETE FROM restaurant_tables WHERE tableId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, tableId);

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
	            return true;
	        }

	    } catch (SQLException e) {
	        // This will print if you try to delete a table that has reservations linked to it
	        System.out.println("Error deleting table: " + e.getMessage());
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, null);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return false;
	}
	
	
	/**
	 * Retrieves the entire waiting list, sorted by entry time (oldest first).
	 * @return An ArrayList of rows, where each row is an ArrayList of objects.
	 */
	public ArrayList<ArrayList<Object>> getWaitingListQuery() {
	    ArrayList<ArrayList<Object>> waitingList = new ArrayList<>();

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
	        // Order by entryTime ASC ensures FIFO (First In, First Out)
	        String query = "SELECT * FROM waiting_list ORDER BY entryTime ASC";

	        ps = conn.prepareStatement(query);
	        rs = ps.executeQuery();

	        while (rs.next()) {
	            ArrayList<Object> waiter = new ArrayList<>();
	            
	            // We use the exact column names from your CREATE TABLE definition
	            waiter.add(rs.getInt("confirmationCode")); 
	            waiter.add(rs.getInt("customerId"));       
	            waiter.add(rs.getInt("numberOfDiners"));   
	            waiter.add(rs.getString("status"));        
	            waiter.add(rs.getTimestamp("entryTime"));  

	            waitingList.add(waiter);
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }
	    
	    return waitingList;
	}
	
	
	/**
	 * Retrieves table details by tableId and updates the Table object.
	 * @param table The Table object containing the tableId.
	 * @return The updated Table object, or null if the table was not found.
	 */
	public Table getTableByTableIdQuery(Table table) {
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
	        String query = "SELECT * FROM restaurant_tables WHERE tableId = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, table.getTableId());

	        rs = ps.executeQuery();

	        if (rs.next()) {
	            // Using exact column names from restaurant_tables
	            table.setSeatsNumber(rs.getInt("seatsNumber"));
	            table.setLocation(rs.getString("location")); 
	            table.setStatus(rs.getString("status"));     
	            
	            return table; // Return the updated object
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        closeResources(ps, rs);
	        releaseConnection(pConn); // Release back to pool
	    }

	    return null; // Return null if not found
	}
	
	
	
	/**
	 * Updates the status of a customer in the waiting list to 'seated'.
	 * @param waiter The WaitList object containing the confirmation code.
	 * @return true if successful, false otherwise.
	 */
	public boolean updateStatusInWaitingListQuery(WaitList waiter) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        // Using "status" and "confirmationCode" columns
	        String query = "UPDATE waiting_list SET status = 'seated' WHERE confirmationCode = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, waiter.getConfirmationCode());

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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
	 * Checks if a specific confirmation code already exists in the waiting_list table.
	 * @param code The confirmation code to check.
	 * @return true if the code exists, false otherwise.
	 */
	public boolean checkIfConfCodeExistsInWaitingList(int code) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;
	    ResultSet rs = null;

	    try {
	        String query = "SELECT confirmationCode FROM waiting_list WHERE confirmationCode = ?";

	        ps = conn.prepareStatement(query);
	        ps.setInt(1, code);

	        rs = ps.executeQuery();

	        // If rs.next() is true, the code exists
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
	 * Inserts a new customer into the waiting list.
	 * @param newWait The WaitList object containing customerId, numberOfDiners, and confirmationCode.
	 * @return true if the insertion was successful, false otherwise.
	 */
	public boolean createNewWaitQuery(WaitList newWait) {
	    // 1. Get connection from the pool
	    PooledConnection pConn = this.getConnection();

	    // Safety check
	    if (pConn == null) {
	        return false;
	    }

	    Connection conn = pConn.getConnection();
	    PreparedStatement ps = null;

	    try {
	        String query = "INSERT INTO waiting_list (customerId, numberOfDiners, confirmationCode) VALUES (?, ?, ?)";

	        ps = conn.prepareStatement(query);
	        
	        ps.setInt(1, newWait.getCustomerId());
	        ps.setInt(2, newWait.getNumberOfDiners());
	        ps.setInt(3, newWait.getConfirmationCode());

	        int rowsAffected = ps.executeUpdate();

	        if (rowsAffected > 0) {
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