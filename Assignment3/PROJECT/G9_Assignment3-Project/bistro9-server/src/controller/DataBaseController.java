package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
import data.OpeningHours;
import data.OpeningHoursPerDay;
import data.Subscriber;
import data.SubscriberReport;
import data.Table;
import data.TableReservation;
import data.TimeReport;
import data.TimeSlot;
import data.WaitList;

public class DataBaseController {

	// START OF API:
	// .
	// The following public methods
	// allow controllers to retrieve
	// or modify data.
	// .
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
	// 35. checkIfConfCodeExistsInWaitingList(int) : boolean
	// 36. updateStatusAndExitTimeInWaitingListQuery(WaitList) : boolean
	// 37. deleteFromWaitList(WaitList) : boolean
	// 38. isTableNeededQueue(int) : boolean
	// 39. addToWaitList(WaitList) : boolean
	// 40. getAllSpecialDaysQuery(ArrayList<LocalDate>) : boolean
	// 41. getWeeklyOpeningTimeForSpecificDayQuery(OpeningHours) : boolean
	// 42. getSubscriberReportByRangeDateQuery(SubscriberReport) : boolean
	// 43. getDailyTotalReservationsQuery(LocalDate) : int
	// 44. getDailyTotalWaitingQuery(LocalDate) : int
	// 45. addSubscriberReportQuery(SubscriberReport) : boolean
	// 46. getTimeReportByRangeDateQuery(TimeReport) : boolean
	// 47. getDailyAvgArrivalQuery(LocalDate) : Integer
	// 48. getDailyAvgLeavingQuery(LocalDate) : Integer
	// 49. addTimeReportQuery(TimeReport) : boolean
	// 50. deleteFromWaitListByReservationIdQuery(int) : boolean
	// 51. checkReportExistsQuery(LocalDate, LocalDate, String) : boolean
	// 52. updateReservationDateQuery(int, Timestamp) : boolean
	// 53. getReservationIdByBillIdQuery(Bill) : int
	// 54. payBillQuery(Bill) : boolean
	// 55. updateOpeningTimeQuery(OpeningHours) : boolean
	// 56. deleteOpeningTimeQuery(OpeningHours) : boolean
	// 57. deleteSpecialOpeningTimeQuery(OpeningHoursPerDay) : boolean
	// 58. updateBillAmountsQuery(Bill) : boolean
	// 59. closeRestaurantOnSpecialDayQuery(OpeningHoursPerDay) : boolean
	// 60. updateSpecialOpeningTimeQuery(OpeningHoursPerDay, OpeningHoursPerDay) : boolean
	// 61. updateOpeningTimeQuery(OpeningHours, OpeningHours) : boolean
	// 62. getReservationsByDateRangeQuery(Timestamp, Timestamp) : ArrayList<ArrayList<Object>>
	// 63. getReservationsByAttributeQuery(String, Object) : ArrayList<ArrayList<Object>>
	// .
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

	// 1
	/**
	 * Deletes specific special opening hours for a specific date. Implements STRICT
	 * "All-or-Nothing" logic: If even ONE slot is not found, it stops, rolls back,
	 * and returns false. * @param openingHours The OpeningHoursPerDay object
	 * containing the LocalDate and list of slots.
	 * 
	 * @return true if ALL slots were successfully deleted, false otherwise.
	 */
	public boolean deleteSpecialOpeningTimeQuery(OpeningHoursPerDay openingHours) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		boolean success = false;

		try {
			conn.setAutoCommit(false); // Start Transaction

			String query = "DELETE FROM special_hours WHERE specificDate = ? AND openingTime = ? AND closingTime = ?";
			ps = conn.prepareStatement(query);

			for (TimeSlot slot : openingHours.getSlots()) {
				// 1. Set Date (Converted from LocalDate to sql.Date)
				ps.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));

				// 2. Set Opening Time
				ps.setTime(2, java.sql.Time.valueOf(slot.getOpen()));

				// 3. Set Closing Time
				ps.setTime(3, java.sql.Time.valueOf(slot.getClose()));

				int rowsAffected = ps.executeUpdate();

				// OPTIMIZATION: Early Exit
				// If the row doesn't exist, we stop immediately and undo everything.
				if (rowsAffected == 0) {
					System.out.println("Error: Special Slot " + openingHours.getDay() + " " + slot.getOpen()
							+ " not found. Rolling back.");
					conn.rollback();
					return false;
				}
			}

			// If we reached here, ALL deletions were successful
			conn.commit();
			success = true;

		} catch (SQLException e) {
			System.out.println("Error deleting special hours: " + e.getMessage());
			e.printStackTrace();
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return false;
		} finally {
			// Always restore AutoCommit and release connection
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			closeResources(ps, null);
			releaseConnection(pConn);
		}

		return success;
	}

	// 2
	/**
	 * Deletes specific weekly opening hours. Implements STRICT "All-or-Nothing"
	 * logic: If even ONE slot is not found (rowsAffected == 0), it immediately
	 * stops, rolls back, and returns false. * @param openingHours The OpeningHours
	 * object containing the day string and list of slots to delete.
	 * 
	 * @return true if ALL slots were successfully deleted, false otherwise.
	 */
	public boolean deleteOpeningTimeQuery(OpeningHours openingHours) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		boolean success = false;

		try {
			conn.setAutoCommit(false); // Start Transaction

			String query = "DELETE FROM weekly_hours WHERE dayOfWeek = ? AND openingTime = ? AND closingTime = ?";
			ps = conn.prepareStatement(query);

			for (TimeSlot slot : openingHours.getSlots()) {
				ps.setString(1, openingHours.getDay());
				ps.setTime(2, java.sql.Time.valueOf(slot.getOpen()));
				ps.setTime(3, java.sql.Time.valueOf(slot.getClose()));

				int rowsAffected = ps.executeUpdate();

				// OPTIMIZATION: Early Exit
				// Even though we return here, the 'finally' block WILL run first.
				if (rowsAffected == 0) {
					System.out.println(
							"Error: Slot " + slot.getOpen() + "-" + slot.getClose() + " not found. Rolling back.");
					conn.rollback();
					return false;
				}
			}

			// If we reached here, it means ALL updates returned 1 (success)
			conn.commit();
			success = true;

		} catch (SQLException e) {
			System.out.println("Error deleting weekly hours: " + e.getMessage());
			e.printStackTrace();
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return false;
		} finally {
			// This block ALWAYS runs, even after 'return false' above.
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			closeResources(ps, null);
			releaseConnection(pConn);
		}

		return success;
	}

	// 3
	/**
	 * Adds new weekly opening hours for a specific day of the week. This method
	 * inserts new slots without deleting existing ones. * @param openingHours The
	 * OpeningHours object containing the day string and list of slots.
	 * 
	 * @return true if the insertion was successful, false otherwise.
	 */
	public boolean updateOpeningTimeQuery(OpeningHours openingHours) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		boolean success = false;

		try {
			// START TRANSACTION
			conn.setAutoCommit(false);

			String query = "INSERT INTO weekly_hours (dayOfWeek, openingTime, closingTime) VALUES (?, ?, ?)";
			ps = conn.prepareStatement(query);

			// Loop through all slots in the list and queue them for insertion
			for (TimeSlot slot : openingHours.getSlots()) {
				// 1. Set Day (String, matching the ENUM 'SUNDAY', 'MONDAY' etc.)
				ps.setString(1, openingHours.getDay());

				// 2. Set Opening Time
				ps.setTime(2, java.sql.Time.valueOf(slot.getOpen()));

				// 3. Set Closing Time
				ps.setTime(3, java.sql.Time.valueOf(slot.getClose()));

				ps.executeUpdate();
			}

			// COMMIT TRANSACTION (Write changes to DB)
			conn.commit();
			success = true;

		} catch (SQLException e) {
			System.out.println("Error adding weekly hours (Possible duplicate or invalid day): " + e.getMessage());
			e.printStackTrace();
			try {
				// ROLLBACK on error
				if (conn != null) {
					conn.rollback();
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		} finally {
			// Restore AutoCommit
			try {
				if (conn != null) {
					conn.setAutoCommit(true);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

			closeResources(ps, null);
			releaseConnection(pConn); // Release back to pool
		}

		return success;
	}

	// 4
	/**
	 * Updates an existing bill record with payment details, amounts, and discount
	 * info.
	 *
	 * @param bill The fully populated Bill object.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean payBillQuery(Bill bill) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;
		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {

			String query = "UPDATE bills SET isPaid = ?, paymentMethod = ? WHERE billId = ?";

			ps = conn.prepareStatement(query);

			ps.setBoolean(1, bill.isPaid()); // true
			ps.setString(2, bill.getPaymentMethod()); // "credit"
			ps.setInt(3, bill.getBillId()); // WHERE clause

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}
		return false;
	}

	// 5
	/**
	 * Updates the financial details (totals and discounts) of a specific bill in
	 * the database.
	 *
	 * @param bill The Bill object containing the billId and the new amount/discount
	 *             values to be updated.
	 * @return true if the update was successful (the bill existed and was
	 *         modified), false otherwise.
	 */
	public boolean updateBillAmountsQuery(Bill bill) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;
		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			String query = "UPDATE bills SET totalAmount = ?, totalAmountAfterDiscount = ?, "
					+ "discountPercentage = ?, discountType = ? WHERE billId = ?";

			ps = conn.prepareStatement(query);
			ps.setDouble(1, bill.getTotalAmount());
			ps.setDouble(2, bill.getTotalAmountAfterDiscount());
			ps.setDouble(3, bill.getDiscountSize());
			ps.setString(4, bill.getDiscountType());
			ps.setInt(5, bill.getBillId());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}
		return false;
	}

	// 6
	/**
	 * Retrieves the reservation ID associated with a specific bill ID.
	 *
	 * @param bill The Bill object containing the billId.
	 * @return The reservationId associated with the bill, or 0 if not found.
	 */
	public int getReservationIdByBillIdQuery(Bill bill) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return 0;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT reservationId FROM bills WHERE billId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, bill.getBillId());

			rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt("reservationId");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return 0;
	}

	// 7
	/**
	 * Updates the reservation date and time for a specific reservation.
	 *
	 * @param reservationId The ID of the reservation to update.
	 * @param newDate       The new Timestamp (Date and Time) to set.
	 * @return true if the update was successful, false otherwise.
	 */
	public boolean updateReservationDateQuery(int reservationId, java.sql.Timestamp newDate) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// SQL Update query
			String query = "UPDATE table_reservations SET reservationDate = ? WHERE reservationId = ?";

			ps = conn.prepareStatement(query);
			ps.setTimestamp(1, newDate); // Set the new date/time
			ps.setInt(2, reservationId); // Identify the row

			int rowsAffected = ps.executeUpdate();

			// If rowsAffected > 0, the update succeeded
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

	// 8
	/**
	 * Checks if a report already exists in the report_manager table for the given
	 * date range and type.
	 *
	 * @param startDay   The start date of the report.
	 * @param endDay     The end date of the report.
	 * @param reportType The type of report ('subscriber' or 'time').
	 * @return true if the report exists, false otherwise.
	 */
	public boolean checkReportExistsQuery(LocalDate startDay, LocalDate endDay, String reportType) {
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
			// Query checks based on the Unique Key: (startDay, endDay, reportType)
			String query = "SELECT 1 FROM report_manager WHERE startDay = ? AND endDay = ? AND reportType = ?";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(startDay));
			ps.setDate(2, java.sql.Date.valueOf(endDay));
			ps.setString(3, reportType);

			rs = ps.executeQuery();

			// If a row is returned, the report exists
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

	// 9
	/**
	 * Cancels a waiting list entry by setting status to 'cancelled' based on the
	 * reservation ID. This is used when a reservation is auto-cancelled due to
	 * lateness.
	 *
	 * @param reservationId The reservation ID linked to the waiting list entry.
	 * @return true if an entry was found and updated, false otherwise.
	 */
	public boolean deleteFromWaitListByReservationIdQuery(int reservationId) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// Update status to 'cancelled' where reservationId matches
			String query = "UPDATE waiting_list SET status = 'cancelled', exitTimeFromList = CURRENT_TIMESTAMP WHERE reservationId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, reservationId);

			int rowsAffected = ps.executeUpdate();

			// If rowsAffected > 0, it means the entry existed and was updated
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

	// 10
	/**
	 * Calculates the average arrival delay (Actual Arrival - Scheduled Time) in
	 * minutes for a specific date. Only considers 'completed' reservations.
	 * * @param date The date to analyze.
	 * 
	 * @return The average delay in minutes (rounded). Returns null if an error
	 *         occurs.
	 */
	public Integer getDailyAvgArrivalQuery(LocalDate date) {
		PooledConnection pConn = this.getConnection();

		// DEBUG: Print if connection failed
		if (pConn == null) {
			System.err.println("CRITICAL ERROR: Could not get connection for date: " + date);
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, reservationDate, arrivalTime))) "
					+ "FROM table_reservations " + "WHERE DATE(reservationDate) = ? AND status = 'completed'";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(date));

			rs = ps.executeQuery();

			if (rs.next()) {
				int result = rs.getInt(1);
				System.out.println("Avg Arrival Delay for " + date + ": " + result + " minutes");
				return result; // Autoboxing to Integer
			}

		} catch (SQLException e) {
			System.err.println("SQL CRASH on Date: " + date);
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return null; // Return null on error
	}

	// 11
	/**
	 * Calculates the average overstay duration (Leaving Time - (Arrival Time + 2
	 * Hours)) in minutes. Only considers 'completed' reservations. * @param date
	 * The date to analyze.
	 * 
	 * @return The average overstay in minutes (rounded). Returns null if an error
	 *         occurs.
	 */
	public Integer getDailyAvgLeavingQuery(LocalDate date) {
		PooledConnection pConn = this.getConnection();

		// DEBUG: Print if connection failed
		if (pConn == null) {
			System.err.println("CRITICAL ERROR: Could not get connection for Leaving Time Query on date: " + date);
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "SELECT ROUND(AVG(TIMESTAMPDIFF(MINUTE, DATE_ADD(arrivalTime, INTERVAL 2 HOUR), leavingTime))) "
					+ "FROM table_reservations " + "WHERE DATE(reservationDate) = ? AND status = 'completed'";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(date));

			rs = ps.executeQuery();

			if (rs.next()) {
				int result = rs.getInt(1);
				System.out.println("Avg Overstay for " + date + ": " + result + " minutes");
				return result; // Autoboxing to Integer
			}

		} catch (SQLException e) {
			System.err.println("SQL CRASH in Leaving Time Query on Date: " + date);
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return null; // Return null on error
	}

	// 12
	/**
	 * Inserts a new time report into the database. Uses a Transaction to ensure
	 * both report_manager and time_report tables are updated correctly.
	 * 
	 * @param report The TimeReport object containing metadata and rows.
	 * @return true if the transaction was successful, false otherwise.
	 */
	public boolean addTimeReportQuery(TimeReport report) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement psManager = null;
		PreparedStatement psDetails = null;
		ResultSet rs = null;

		try {
			// START TRANSACTION
			conn.setAutoCommit(false);

			// STEP A: Insert into report_manager
			String queryManager = "INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES (?, ?, ?, ?)";

			psManager = conn.prepareStatement(queryManager, Statement.RETURN_GENERATED_KEYS);
			psManager.setDate(1, java.sql.Date.valueOf(report.getStartDay()));
			psManager.setDate(2, java.sql.Date.valueOf(report.getEndDay()));
			psManager.setString(3, report.getReportRange()); // 'monthly'
			psManager.setString(4, report.getReportType()); // 'time'

			int affected = psManager.executeUpdate();

			if (affected == 0) {
				conn.rollback();
				return false;
			}

			// Retrieve generated reportId
			rs = psManager.getGeneratedKeys();
			int reportId = -1;
			if (rs.next()) {
				reportId = rs.getInt(1);
			} else {
				conn.rollback();
				return false;
			}

			// STEP B: Insert all rows into time_report
			String queryDetails = "INSERT INTO time_report (reportId, reportDate, avgArrival, avgLeaving) VALUES (?, ?, ?, ?)";
			psDetails = conn.prepareStatement(queryDetails);

			for (TimeReport.Row row : report.getRows()) {
				psDetails.setInt(1, reportId);
				psDetails.setDate(2, java.sql.Date.valueOf(row.getReportDate()));
				psDetails.setInt(3, row.getAvgArrival());
				psDetails.setInt(4, row.getAvgLeaving());

				psDetails.addBatch(); // Add to batch for efficiency
			}

			// Execute Batch
			psDetails.executeBatch();

			// COMMIT TRANSACTION
			conn.commit();
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			try {
				if (conn != null)
					conn.rollback(); // Rollback on error
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		} finally {
			try {
				if (conn != null)
					conn.setAutoCommit(true); // Restore default
			} catch (SQLException e) {
				e.printStackTrace();
			}
			closeResources(psManager, rs);
			closeResources(psDetails, null);
			releaseConnection(pConn); // Release back to pool
		}

		return false;
	}

	// 13
	/**
	 * Retrieves the time report data for a specific date range by fetching
	 * pre-saved data directly from the time_report table. * @param report The
	 * TimeReport object containing startDay and endDay.
	 * 
	 * @return true if the query executed successfully, false otherwise.
	 */
	public boolean getTimeReportByRangeDateQuery(TimeReport report) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// SIMPLIFIED QUERY: Direct access to time_report without JOIN
			String query = "SELECT reportDate, avgArrival, avgLeaving " + "FROM time_report "
					+ "WHERE reportDate BETWEEN ? AND ? " + "ORDER BY reportDate ASC";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(report.getStartDay()));
			ps.setDate(2, java.sql.Date.valueOf(report.getEndDay()));

			rs = ps.executeQuery();

			while (rs.next()) {
				LocalDate date = rs.getDate("reportDate").toLocalDate();
				int avgArrival = rs.getInt("avgArrival");
				int avgLeaving = rs.getInt("avgLeaving");

				// Add the pre-calculated row to the report object
				report.addRow(date, avgArrival, avgLeaving);
			}

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return false;
	}

	// 14
	/**
	 * Counts total 'completed' reservations for a specific date.
	 * 
	 * @param date The date to check.
	 * @return The count of reservations (0 or more), or -1 if an error occurred.
	 */
	public int getDailyTotalReservationsQuery(LocalDate date) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return -1;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Count reservations where the date part matches and status is 'completed'
			String query = "SELECT COUNT(*) FROM table_reservations WHERE DATE(reservationDate) = ? AND status = 'completed'";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(date));

			rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return -1; // Return -1 on error
	}

	// 15
	/**
	 * Counts total 'seated' customers from the waiting list for a specific date.
	 * 
	 * @param date The date to check.
	 * @return The count of seated customers (0 or more), or -1 if an error
	 *         occurred.
	 */
	public int getDailyTotalWaitingQuery(LocalDate date) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return -1;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Count waiting list entries where entry date matches and status is 'seated'
			// Note: Using 'entryTimeToList' as defined in your latest schema
			String query = "SELECT COUNT(*) FROM waiting_list WHERE DATE(entryTimeToList) = ? AND status = 'seated'";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(date));

			rs = ps.executeQuery();

			if (rs.next()) {
				return rs.getInt(1);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return -1; // Return -1 on error
	}

	// 16
	/**
	 * Inserts a new subscriber report into the database. Uses a Transaction to
	 * ensure both report_manager and subscriber_report tables are updated
	 * correctly.
	 * 
	 * @param report The SubscriberReport object containing metadata and rows.
	 * @return true if the transaction was successful, false otherwise.
	 */
	public boolean addSubscriberReportQuery(SubscriberReport report) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement psManager = null;
		PreparedStatement psDetails = null;
		ResultSet rs = null;

		try {
			// START TRANSACTION
			conn.setAutoCommit(false);

			// STEP A: Insert into report_manager
			String queryManager = "INSERT INTO report_manager (startDay, endDay, reportRange, reportType) VALUES (?, ?, ?, ?)";

			psManager = conn.prepareStatement(queryManager, Statement.RETURN_GENERATED_KEYS);
			psManager.setDate(1, java.sql.Date.valueOf(report.getStartDay()));
			psManager.setDate(2, java.sql.Date.valueOf(report.getEndDay()));
			psManager.setString(3, report.getReportRange()); // 'monthly'
			psManager.setString(4, report.getReportType()); // 'subscriber'

			int affected = psManager.executeUpdate();

			if (affected == 0) {
				conn.rollback();
				return false;
			}

			// Retrieve generated reportId
			rs = psManager.getGeneratedKeys();
			int reportId = -1;
			if (rs.next()) {
				reportId = rs.getInt(1);
			} else {
				conn.rollback();
				return false;
			}

			// STEP B: Insert all rows into subscriber_report
			String queryDetails = "INSERT INTO subscriber_report (reportId, reportDate, totalReservations, totalWaiting) VALUES (?, ?, ?, ?)";
			psDetails = conn.prepareStatement(queryDetails);

			for (SubscriberReport.Row row : report.getRows()) {
				psDetails.setInt(1, reportId);
				psDetails.setDate(2, java.sql.Date.valueOf(row.getReportDate()));
				psDetails.setInt(3, row.getTotalReservations());
				psDetails.setInt(4, row.getTotalWaiting());

				psDetails.addBatch(); // Add to batch for efficiency
			}

			// Execute Batch
			psDetails.executeBatch();

			// COMMIT TRANSACTION
			conn.commit();
			return true;

		} catch (SQLException e) {
			e.printStackTrace();
			try {
				if (conn != null)
					conn.rollback(); // Rollback on error
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		} finally {
			try {
				if (conn != null)
					conn.setAutoCommit(true); // Restore default
			} catch (SQLException e) {
				e.printStackTrace();
			}
			closeResources(psManager, rs);
			closeResources(psDetails, null);
			releaseConnection(pConn); // Release back to pool
		}

		return false;
	}

	// 17
	/**
	 * Retrieves the subscriber report data for a specific date range by fetching
	 * pre-saved data directly from the subscriber_report table. * @param report The
	 * SubscriberReport object containing startDay and endDay.
	 * 
	 * @return true if the query executed successfully, false otherwise.
	 */
	public boolean getSubscriberReportByRangeDateQuery(SubscriberReport report) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// SIMPLIFIED QUERY: Direct access to subscriber_report without JOIN
			String query = "SELECT reportDate, totalReservations, totalWaiting " + "FROM subscriber_report "
					+ "WHERE reportDate BETWEEN ? AND ? " + "ORDER BY reportDate ASC";

			ps = conn.prepareStatement(query);
			ps.setDate(1, java.sql.Date.valueOf(report.getStartDay()));
			ps.setDate(2, java.sql.Date.valueOf(report.getEndDay()));

			rs = ps.executeQuery();

			while (rs.next()) {
				LocalDate date = rs.getDate("reportDate").toLocalDate();
				int totalRes = rs.getInt("totalReservations");
				int totalWait = rs.getInt("totalWaiting");

				// Add the pre-calculated row to the report object
				report.addRow(date, totalRes, totalWait);
			}

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return false;
	}

	// 18
	/**
	 * Retrieves the standard weekly opening hours for a specific day of the week.
	 * Populates the slots in the passed OpeningHours object.
	 * 
	 * @param openingHours The OpeningHours object containing the day name (e.g.,
	 *                     "SUNDAY").
	 * @return true if the query was successful (even if the day is closed), false
	 *         on DB error.
	 */
	public boolean getWeeklyOpeningTimeForSpecificDayQuery(OpeningHours openingHours) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		// Create a list to hold the found slots
		ArrayList<TimeSlot> foundSlots = new ArrayList<>();

		try {
			String query = "SELECT openingTime, closingTime FROM weekly_hours WHERE dayOfWeek = ?";

			ps = conn.prepareStatement(query);
			// Assuming openingHours.getDay() returns the String name like "SUNDAY"
			ps.setString(1, openingHours.getDay());

			rs = ps.executeQuery();

			while (rs.next()) {
				java.sql.Time sqlOpen = rs.getTime("openingTime");
				java.sql.Time sqlClose = rs.getTime("closingTime");

				if (sqlOpen != null && sqlClose != null) {
					// Only add if start time is DIFFERENT from end time.
					// If 00:00 to 00:00, it effectively means closed for that slot.
					if (!sqlOpen.equals(sqlClose)) {
						foundSlots.add(new TimeSlot(sqlOpen.toLocalTime(), sqlClose.toLocalTime()));
					}
				}
			}

			// Update the object with the list of slots we found.
			// If the list is empty, it correctly represents a closed day.
			openingHours.setSlots(foundSlots);

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return false;
	}

	// 19
	/**
	 * Retrieves all dates that have special hours defined in the database.
	 * 
	 * @param datesList An empty ArrayList to be populated with LocalDate objects.
	 * @return true if the query was successful, false otherwise.
	 */
	public boolean getAllSpecialDaysQuery(ArrayList<LocalDate> datesList) {
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
			// We use DISTINCT to ensure we get each date only once, even if it has multiple
			// shifts
			String query = "SELECT DISTINCT specificDate FROM special_hours ORDER BY specificDate ASC";

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				java.sql.Date sqlDate = rs.getDate("specificDate");

				if (sqlDate != null) {
					// Convert java.sql.Date to java.time.LocalDate and add to list
					datesList.add(sqlDate.toLocalDate());
				}
			}

			return true;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return false;
	}

	// 20
	/**
	 * Retrieves all reservation records associated with a specific customer ID from
	 * the database.
	 *
	 * @param customerId The unique identifier of the customer whose reservations
	 *                   are to be fetched.
	 * @return An ArrayList of ArrayLists, where each inner list contains the raw
	 *         data fields of a single reservation, or null if the database
	 *         connection fails.
	 */
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

	// 21
	/**
	 * Retrieves all reservation records scheduled for a specific date from the
	 * database. This method filters by the date part of the reservation timestamp,
	 * ignoring the specific time.
	 *
	 * @param day The LocalDate representing the day for which to retrieve
	 *            reservations.
	 * @return An ArrayList of ArrayLists, where each inner list contains the raw
	 *         data fields of a single reservation, or null if the database
	 *         connection fails.
	 */
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

	// 22
	/**
	 * Retrieves the effective opening hours for a specific date, prioritizing
	 * specific overrides over the general schedule.
	 * <p>
	 * Logic Flow: 1. <b>Special Hours:</b> Checks for specific date overrides
	 * first. If an entry exists where start equals end time, the day is marked as
	 * explicitly CLOSED. 2. <b>Weekly Hours:</b> If no special hours exist for the
	 * date, falls back to the standard weekly schedule for that day of the week.
	 * </p>
	 *
	 * @param openingHours The OpeningHoursPerDay object containing the date to
	 *                     query.
	 * @return The same OpeningHoursPerDay object populated with the found
	 *         TimeSlots, or with a null list if the restaurant is closed or on
	 *         error.
	 */
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
					// --- NEW LOGIC START ---
					// If Special Hours has start == end, it implies the restaurant is explicitly
					// CLOSED for this date.
					// We return null immediately and DO NOT check weekly hours.
					if (sqlOpen.equals(sqlClose)) {
						openingHours.setSlots(null);
						return openingHours; // Exit immediately
					}
					// --- NEW LOGIC END ---

					// Otherwise, it's a valid special opening time, add it.
					foundSlots.add(new TimeSlot(sqlOpen.toLocalTime(), sqlClose.toLocalTime()));
				}
			}

			// Close resources from the first query to prepare for the second
			rs.close();
			ps.close();

			// ---------------------------------------------------------
			// STEP 2: If no special hours found, check Weekly Hours
			// ---------------------------------------------------------
			// If we reached here, it means we didn't hit an explicit "Closed" special hour.
			// If foundSlots is empty, it means there were NO special hours records at all.
			if (foundSlots.isEmpty()) {
				String queryWeekly = "SELECT openingTime, closingTime FROM weekly_hours WHERE dayOfWeek = ?";
				ps = conn.prepareStatement(queryWeekly);

				// Since DB is now uppercase ENUM ('SUNDAY'), we use Java's default toString()
				ps.setString(1, openingHours.getDay().getDayOfWeek().toString());

				rs = ps.executeQuery();

				while (rs.next()) {
					java.sql.Time sqlOpen = rs.getTime("openingTime");
					java.sql.Time sqlClose = rs.getTime("closingTime");

					if (sqlOpen != null && sqlClose != null) {
						// Weekly Logic: If times are equal (00:00-00:00), we just skip adding it.
						// We check other rows in case of split shifts.
						if (!sqlOpen.equals(sqlClose)) {
							foundSlots.add(new TimeSlot(sqlOpen.toLocalTime(), sqlClose.toLocalTime()));
						}
					}
				}
			}

			// ---------------------------------------------------------
			// STEP 3: Final Decision
			// ---------------------------------------------------------
			// If the list is empty at this point, it means the restaurant is closed
			// (either no weekly hours defined, or weekly hours were 00:00-00:00).
			if (foundSlots.isEmpty()) {
				openingHours.setSlots(null);
			} else {
				openingHours.setSlots(foundSlots);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn); // Release back to pool
		}

		return openingHours;
	}

	// 23
	/**
	 * Retrieves the complete list of all physical tables defined in the restaurant
	 * configuration.
	 *
	 * @return An ArrayList of ArrayLists, where each inner list contains the
	 *         details (ID, seat count, location, status) of a single table, or null
	 *         if the database connection fails.
	 */
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

	// 24
	/**
	 * Checks the login details of a subscriber against the database.
	 * 
	 * @param sub The Subscriber object containing the username and password to
	 *            check.
	 * @return The Subscriber object with full details if credentials are correct,
	 *         null otherwise.
	 */
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

	// 25
	// updated 31/12/25 to logically delete by changing
	// the status to cancelled.
	/**
	 * Cancels a reservation by updating its status to 'cancelled' based on the
	 * confirmation code.
	 * 
	 * @param confirmationCode The unique confirmation code of the reservation.
	 * @return true if the reservation was found and status updated, false
	 *         otherwise.
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
			// CHANGED: Update status instead of DELETE
			String query = "UPDATE table_reservations SET status = 'cancelled' WHERE confirmationCode = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, confirmationCode);

			// executeUpdate returns the number of rows affected
			int rowsAffected = ps.executeUpdate();

			// If rowsAffected > 0, it means the reservation was found and updated
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

	// 26
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

	// 27
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

	// 28
	/**
	 * Inserts a new reservation record into the database and updates the provided
	 * object with server-generated fields.
	 * <p>
	 * Upon successful insertion, this method automatically: 1. Retrieves the
	 * auto-generated <b>reservationId</b>. 2. Fetches the default <b>status</b> and
	 * <b>creation timestamp</b> assigned by the database. 3. Updates the passed
	 * TableReservation object with these values.
	 * </p>
	 *
	 * @param res The TableReservation object containing the reservation details
	 *            (customer, time, diners).
	 * @return true if the reservation was successfully created and the object
	 *         updated, false on database error.
	 */
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

	// 29
	/**
	 * Checks if a specific confirmation code already exists in the database. Used
	 * to ensure that generated confirmation codes are unique.
	 * 
	 * @param code The confirmation code to check.
	 * @return true if the code already exists, false if it is unique.
	 */
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

	// 30
	/**
	 * Retrieves the customer ID for a given customer based on their phone number or
	 * email. If the customer does not exist, a new record is created in the
	 * database.
	 * 
	 * @param cust The Customer object containing phone number and email.
	 * @return The customer ID if found or created, -1 on failure.
	 */
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

	// STATIC METHOD
	/**
	 * Utility method to safely close JDBC resources (Statement and ResultSet).
	 * <p>
	 * This method performs null checks and suppresses any SQLExceptions that might
	 * occur during closing. This ensures that exceptions in the
	 * <code>finally</code> block do not mask the original exception from the
	 * <code>try</code> block.
	 * </p>
	 *
	 * @param stmt The PreparedStatement to close.
	 * @param rs   The ResultSet to close.
	 */
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

	// 31
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

	// 32
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

	// 33
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

	// 34
	/**
	 * Retrieves the discount percentage for a specific customer type.
	 * 
	 * @param type The type of customer (e.g., 'subscriber', 'customer').
	 * @return The discount percentage as a float (e.g., 10.0 for 10%), or 0.0 if
	 *         not found.
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

	// 35
	/**
	 * Inserts a new bill record into the database.
	 * 
	 * @param bill The Bill object containing reservationId, amounts, and discount
	 *             info.
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

	// 36
	/**
	 * Retrieves reservation details using the confirmation code. Updates the passed
	 * TableReservation object with the data found.
	 * 
	 * @param res The TableReservation object containing the confirmation code.
	 * @return true if the reservation was found and object updated, false
	 *         otherwise.
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

	// 37
	/**
	 * Retrieves bill details using the reservation ID. Updates the passed Bill
	 * object with the data found.
	 * 
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

	// 38
	/**
	 * Updates a bill to status 'Paid' and sets the payment method. Returns the
	 * associated reservationId upon success.
	 * 
	 * @param billId        The ID of the bill to pay.
	 * @param isPaid        The new payment status (true).
	 * @param paymentMethod The method used ('Cash', 'Credit', 'App').
	 * @return The reservationId associated with the bill if successful, or 0 if
	 *         failed.
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

	// 39
	/**
	 * Retrieves reservation details using the reservation ID. Updates the passed
	 * TableReservation object with the data found.
	 * 
	 * @param res The TableReservation object containing the reservationId.
	 * @return true if the reservation was found and object updated, false
	 *         otherwise.
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

	// 40
	/**
	 * Retrieves the customer type string based on the customer ID.
	 * 
	 * @param customerId The ID to check.
	 * @return The type string (e.g., 'subscriber', 'customer') or null if ID not
	 *         found.
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
			String query = "SELECT s.type FROM customer c " + "LEFT JOIN subscriber s ON c.customerId = s.customerId "
					+ "WHERE c.customerId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, customerId);

			rs = ps.executeQuery();

			if (rs.next()) {
				String dbType = rs.getString("type");

				if (dbType != null) {
					// User is in the subscriber table (return 'subscriber', 'restaurant manager',
					// etc.)
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

	// 41
	/**
	 * Updates the opening hours for a specific day by deleting old entries and
	 * inserting new ones (Transaction-based).
	 * 
	 * @param day          The day of the week (e.g., 'SUNDAY').
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

	// 42
	/**
	 * Appends new special opening hours for a specific date.
	 * <p>
	 * <b>Logic Update:</b> Before inserting, this method automatically checks if
	 * the day is currently marked as "Closed" (00:00 - 00:00). If that specific
	 * "Closed" slot exists, it is deleted first to allow the new hours to take
	 * effect.
	 * </p>
	 * * @param openingHours The OpeningHoursPerDay object containing the date and
	 * list of slots to add.
	 * 
	 * @return true if the operation (conditional delete + insertions) was
	 *         successful, false on error.
	 */
	public boolean addNewSpecialOpeningTimeQuery(OpeningHoursPerDay openingHours) {
		// 1. Get connection from the pool
		PooledConnection pConn = this.getConnection();

		// Safety check
		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement psClean = null;
		PreparedStatement psInsert = null;
		boolean success = false;

		try {
			// START TRANSACTION
			conn.setAutoCommit(false);

			// -----------------------------------------------------------------
			// STEP 1: Conditional Clean-up
			// Try to delete the specific "00:00 - 00:00" slot for this date.
			// If it exists, it gets deleted. If not, this does nothing (rowsAffected = 0)
			// and we continue.
			// -----------------------------------------------------------------
			String cleanQuery = "DELETE FROM special_hours WHERE specificDate = ? AND openingTime = '00:00:00' AND closingTime = '00:00:00'";
			psClean = conn.prepareStatement(cleanQuery);
			psClean.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
			psClean.executeUpdate();
			// We don't check the result here because it's okay if it didn't exist.

			// -----------------------------------------------------------------
			// STEP 2: Insert the new time slots
			// -----------------------------------------------------------------
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
			System.out.println("Error adding special hours: " + e.getMessage());
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

			closeResources(psClean, null); // Close the cleanup statement
			closeResources(psInsert, null);
			releaseConnection(pConn); // Release back to pool
		}

		return success;
	}

	// 43
	/**
	 * Retrieves all reservations from the database.
	 * 
	 * @return An ArrayList of rows, where each row is an ArrayList of objects
	 *         representing a reservation.
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

	// 44
	/**
	 * Updates the leaving time for a specific reservation.
	 * 
	 * @param reservationId The ID of the reservation to update.
	 * @param leavingTime   The new timestamp to set as the leaving time.
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

	// 45
	/**
	 * Retrieves all reservations with status 'active' or 'arrived'.
	 * 
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

	// 46
	/**
	 * Retrieves customer details by ID. If the customer is a subscriber, it fetches
	 * the full profile. If the customer is a regular customer, it fetches only
	 * contact info (phone/email).
	 * 
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
			// LEFT JOIN ensures we get customer info even if they are not in the subscriber
			// table
			String query = "SELECT c.phoneNumber, c.email, "
					+ "s.subscriberId, s.firstName, s.lastName, s.username, s.type, s.personalInfo "
					+ "FROM customer c " + "LEFT JOIN subscriber s ON c.customerId = s.customerId "
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

	// 47
	/**
	 * Updates the status of a restaurant table.
	 * 
	 * @param tableId   The ID of the table to update.
	 * @param newStatus The new status string (Must match SQL ENUM: 'available',
	 *                  'reserved', 'occupied').
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

	// 48
	/**
	 * Updates the number of seats for a restaurant table.
	 * 
	 * @param tableId  The ID of the table to update.
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

	// 49
	/**
	 * Updates an existing reservation with new details (Table ID, Arrival Time,
	 * Status).
	 * 
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

			// 1. Table ID (might be null if not assigned yet, but in check-in it should be
			// set)
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

	// 50
	/**
	 * Inserts a new table into the database and retrieves the generated ID.
	 * 
	 * @param table The Table object containing seatsNumber, location, and status.
	 * @return The updated Table object with the new tableId, or null if insertion
	 *         failed.
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

	// 51
	// updated 31/12/25 to only change the status
	// to cancelled instead of real delete.
	/**
	 * "Soft deletes" a table by changing its status to 'cancelled'. This prevents
	 * Foreign Key errors while removing the table from active use.
	 * 
	 * @param tableId The ID of the table to remove.
	 * @return true if the table was found and status updated, false otherwise.
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
			// CHANGED: Update status to 'cancelled' (Soft Delete) instead of DELETE
			String query = "UPDATE restaurant_tables SET status = 'cancelled' WHERE tableId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, tableId);

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

	// 52
	/**
	 * Retrieves the entire waiting list from the database, ordered by entry time
	 * (FIFO).
	 * <p>
	 * The list is sorted in ascending order of <code>entryTimeToList</code>,
	 * ensuring that customers who arrived first appear at the top of the list.
	 * </p>
	 *
	 * @return An ArrayList of ArrayLists, where each inner list contains the raw
	 *         data fields (ID, reservation ref, group size, times, status, type) of
	 *         a single waiting entry, or null on error.
	 */
	public ArrayList<ArrayList<Object>> getWaitingListQuery() {
		ArrayList<ArrayList<Object>> waitingList = new ArrayList<>();
		PooledConnection pConn = this.getConnection();

		if (pConn == null)
			return null;

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Order by entryTime ASC (FIFO queue)
			String query = "SELECT waitingId, reservationId, numberOfDiners, entryTimeToList, exitTimeFromList, status, type "
					+ "FROM waiting_list ORDER BY entryTimeToList ASC";

			ps = conn.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {
				ArrayList<Object> waiter = new ArrayList<>();

				waiter.add(rs.getInt("waitingId")); // Index 0
				waiter.add(rs.getInt("reservationId")); // Index 1
				waiter.add(rs.getInt("numberOfDiners")); // Index 2
				waiter.add(rs.getTimestamp("entryTimeToList")); // Index 3
				waiter.add(rs.getTimestamp("exitTimeFromList")); // Index 4
				waiter.add(rs.getString("status")); // Index 5
				waiter.add(rs.getString("type")); // Index 6

				waitingList.add(waiter);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return waitingList;
	}

	// 53
	/**
	 * Retrieves table details by tableId and updates the Table object.
	 * 
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

	// 54
	/**
	 * Updates the status and exit timestamp for a specific entry in the waiting
	 * list. This is typically used when a customer is moved to a table or leaves
	 * the queue.
	 *
	 * @param waiter The WaitList object containing the waitingId, the new status,
	 *               and the exit time.
	 * @return true if the update was successful (record found and modified), false
	 *         otherwise.
	 */
	public boolean updateStatusAndExitTimeInWaitingListQuery(WaitList waiter) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			String query = "UPDATE waiting_list SET status = ?, exitTimeFromList = ? WHERE waitingId = ?";

			ps = conn.prepareStatement(query);

			ps.setString(1, waiter.getStatus()); // 'seated', 'notified', etc.
			ps.setTimestamp(2, waiter.getExitTimeFromList());
			ps.setInt(3, waiter.getWaitingId());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}
		return false;
	}

	// 55
	/**
	 * Performs a logical deletion of a customer from the waiting list by marking
	 * them as cancelled.
	 * <p>
	 * Instead of physically removing the row from the database, this method updates
	 * the status to 'cancelled' and sets the exit time to the current server
	 * timestamp. This preserves the record for historical analysis.
	 * </p>
	 *
	 * @param waiter The WaitList object containing the ID of the entry to cancel.
	 * @return true if the record was successfully found and updated, false
	 *         otherwise.
	 */
	public boolean deleteFromWaitList(WaitList waiter) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// We use logical deletion (changing status) rather than DELETE FROM
			String query = "UPDATE waiting_list SET status = 'cancelled', exitTimeFromList = CURRENT_TIMESTAMP WHERE waitingId = ?";

			ps = conn.prepareStatement(query);
			ps.setInt(1, waiter.getWaitingId());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}
		return false;
	}

	// 56
	/**
	 * Checks if there are any active entries in the waiting list that can be
	 * accommodated by a specific table capacity.
	 * <p>
	 * This method queries the database to see if any customer with status 'waiting'
	 * has a group size less than or equal to the provided number of seats.
	 * </p>
	 *
	 * @param tableSeats The number of seats available at the table being queried.
	 * @return true if at least one matching waiting group exists, false otherwise.
	 */
	public boolean isTableNeededQueue(int tableSeats) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Check if there is a waiter with status 'waiting' whose group size <=
			// tableSeats
			String query = "SELECT 1 FROM waiting_list WHERE status = 'waiting' AND numberOfDiners <= ? LIMIT 1";

			ps = conn.prepareStatement(query);
			ps.setInt(1, tableSeats);
			rs = ps.executeQuery();

			if (rs.next()) {
				return true; // Someone is waiting for this size
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}
		return false;
	}

	// 57
	/**
	 * Checks if a specific confirmation code already exists in the waiting_list
	 * table. joins waiting_list with table_reservations to find the code.  
	 * 
	 * @param code The confirmation code to check.
	 * @return true if the code exists in the waiting list, false otherwise.
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
			// FIXED QUERY: Join waiting_list and table_reservations
			String query = "SELECT 1 FROM waiting_list w "
					+ "JOIN table_reservations r ON w.reservationId = r.reservationId "
					+ "WHERE r.confirmationCode = ?";

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

	// 58
	/**
	 * Adds a new entry to the restaurant's waiting list.
	 * <p>
	 * This method inserts a record containing the reservation reference, group
	 * size, and type (e.g., 'walk_in' or 'check_in'). Default values for the entry
	 * timestamp and initial status (usually 'waiting') are handled by the database.
	 * </p>
	 *
	 * @param newWait The WaitList object containing the reservationId, number of
	 *                diners, and queue type.
	 * @return true if the insertion was successful, false otherwise.
	 */
	public boolean addToWaitList(WaitList newWait) {
		PooledConnection pConn = this.getConnection();
		if (pConn == null)
			return false;

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// SQL Table waiting_list: (waitingId, reservationId, numberOfDiners,
			// entryTimeToList, exitTimeFromList, status, type)
			// Note: customerId is NOT in this table (it is linked via reservationId)
			String query = "INSERT INTO waiting_list (reservationId, numberOfDiners, type) VALUES (?, ?, ?)";

			ps = conn.prepareStatement(query);

			// 1. reservationId (Must be created before this call)
			ps.setInt(1, newWait.getReservationId());

			// 2. numberOfDiners
			ps.setInt(2, newWait.getNumberOfDiners());

			// 3. type (walk_in / check_in) - Cannot be null
			ps.setString(3, newWait.getType());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}
		return false;
	}

	// 59
	/**
	 * Explicitly closes the restaurant on a specific date by setting its hours to
	 * 00:00-00:00.
	 * <p>
	 * This method performs an atomic transaction: 1. <b>Deletes</b> any existing
	 * special hours entries for the target date (cleaning the slate). 2.
	 * <b>Inserts</b> a single new record with openingTime = 00:00 and closingTime =
	 * 00:00.
	 * </p>
	 *
	 * @param openingHours The OpeningHoursPerDay object containing the target date
	 *                     and the "closed" time slot.
	 * @return true if the operation (delete + insert) was completed successfully,
	 *         false otherwise.
	 */
	public boolean closeRestaurantOnSpecialDayQuery(OpeningHoursPerDay openingHours) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement psDelete = null;
		PreparedStatement psInsert = null;
		boolean success = false;

		try {
			conn.setAutoCommit(false); // Start Transaction

			// STEP 1: Delete ANY existing records for this date
			// We don't check rowsAffected here because it's okay if the day was already
			// empty.
			String deleteQuery = "DELETE FROM special_hours WHERE specificDate = ?";
			psDelete = conn.prepareStatement(deleteQuery);
			psDelete.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
			psDelete.executeUpdate();

			// STEP 2: Insert the "Closed" record (00:00 - 00:00)
			String insertQuery = "INSERT INTO special_hours (specificDate, openingTime, closingTime) VALUES (?, ?, ?)";
			psInsert = conn.prepareStatement(insertQuery);

			for (TimeSlot slot : openingHours.getSlots()) {
				psInsert.setDate(1, java.sql.Date.valueOf(openingHours.getDay()));
				psInsert.setTime(2, java.sql.Time.valueOf(slot.getOpen()));
				psInsert.setTime(3, java.sql.Time.valueOf(slot.getClose()));

				psInsert.executeUpdate();
			}

			conn.commit(); // Commit both changes
			success = true;

		} catch (SQLException e) {
			System.out.println("Error closing restaurant on special day: " + e.getMessage());
			e.printStackTrace();
			try {
				if (conn != null)
					conn.rollback();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			return false;
		} finally {
			try {
				if (conn != null)
					conn.setAutoCommit(true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			closeResources(psDelete, null);
			closeResources(psInsert, null);
			releaseConnection(pConn);
		}

		return success;
	}

	// 60
	/**
	 * Updates a specific special opening hour record, changing its start and end
	 * times.
	 * <p>
	 * This method identifies the row to update by matching the <b>date</b> and the
	 * <b>original</b> opening/closing times. It then updates that specific row with
	 * the <b>new</b> opening and closing times provided.
	 * </p>
	 *
	 * @param oldHours The OpeningHoursPerDay object containing the date and the
	 *                 <b>original</b> time slot (used for the WHERE clause).
	 * @param newHours The OpeningHoursPerDay object containing the <b>new</b> time
	 *                 slot (used for the SET clause).
	 * @return true if the record was found and updated successfully, false
	 *         otherwise.
	 */
	public boolean updateSpecialOpeningTimeQuery(OpeningHoursPerDay oldHours, OpeningHoursPerDay newHours) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// Extract the single time slots from the lists (Logic ensures size is 1)
			TimeSlot oldSlot = oldHours.getSlots().get(0);
			TimeSlot newSlot = newHours.getSlots().get(0);

			String query = "UPDATE special_hours SET openingTime = ?, closingTime = ? "
					+ "WHERE specificDate = ? AND openingTime = ? AND closingTime = ?";

			ps = conn.prepareStatement(query);

			// SET clause (The NEW values)
			ps.setTime(1, java.sql.Time.valueOf(newSlot.getOpen()));
			ps.setTime(2, java.sql.Time.valueOf(newSlot.getClose()));

			// WHERE clause (The OLD values to identify the row)
			ps.setDate(3, java.sql.Date.valueOf(oldHours.getDay()));
			ps.setTime(4, java.sql.Time.valueOf(oldSlot.getOpen()));
			ps.setTime(5, java.sql.Time.valueOf(oldSlot.getClose()));

			int rowsAffected = ps.executeUpdate();

			return rowsAffected > 0;

		} catch (SQLException e) {
			System.out.println("Error updating special opening time: " + e.getMessage());
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}

		return false;
	}

	// 61
	/**
	 * Updates a specific weekly opening hour record (Standard Schedule).
	 * <p>
	 * This method identifies the row to update by matching the <b>day of the
	 * week</b> and the <b>original</b> opening/closing times. It then updates that
	 * specific row with the <b>new</b> opening and closing times provided.
	 * </p>
	 *
	 * @param oldHours The OpeningHours object containing the day string and the
	 *                 <b>original</b> time slot (used for the WHERE clause).
	 * @param newHours The OpeningHours object containing the <b>new</b> time slot
	 *                 (used for the SET clause).
	 * @return true if the record was found and updated successfully, false
	 *         otherwise.
	 */
	public boolean updateOpeningTimeQuery(OpeningHours oldHours, OpeningHours newHours) {
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return false;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;

		try {
			// Extract the single time slots from the lists (Logic ensures size is 1)
			TimeSlot oldSlot = oldHours.getSlots().get(0);
			TimeSlot newSlot = newHours.getSlots().get(0);

			String query = "UPDATE weekly_hours SET openingTime = ?, closingTime = ? "
					+ "WHERE dayOfWeek = ? AND openingTime = ? AND closingTime = ?";

			ps = conn.prepareStatement(query);

			// SET clause (The NEW values)
			ps.setTime(1, java.sql.Time.valueOf(newSlot.getOpen()));
			ps.setTime(2, java.sql.Time.valueOf(newSlot.getClose()));

			// WHERE clause (The OLD values to identify the row)
			ps.setString(3, oldHours.getDay()); // e.g., "SUNDAY"
			ps.setTime(4, java.sql.Time.valueOf(oldSlot.getOpen()));
			ps.setTime(5, java.sql.Time.valueOf(oldSlot.getClose()));

			int rowsAffected = ps.executeUpdate();

			return rowsAffected > 0;

		} catch (SQLException e) {
			System.out.println("Error updating weekly opening time: " + e.getMessage());
			e.printStackTrace();
		} finally {
			closeResources(ps, null);
			releaseConnection(pConn);
		}

		return false;
	}

	// 62
	/**
	 * Retrieves all reservation records that fall within a specific date range.
	 * <p>
	 * The query filters based on the <b>reservationDate</b> column. It is
	 * inclusive, meaning it searches for records where the date is greater than or
	 * equal to the start and less than or equal to the end.
	 * </p>
	 *
	 * @param startDate The beginning of the date range (Timestamp).
	 * @param endDate   The end of the date range (Timestamp).
	 * @return An ArrayList of ArrayLists, where each inner list contains the raw
	 *         data fields of a single reservation. Returns null on connection
	 *         error.
	 */
	public ArrayList<ArrayList<Object>> getReservationsByDateRangeQuery(Timestamp startDate, Timestamp endDate) {
		ArrayList<ArrayList<Object>> reservations = new ArrayList<>();
		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			// Select all reservations where the reservationDate is between the start and
			// end timestamps
			String query = "SELECT * FROM table_reservations WHERE reservationDate >= ? AND reservationDate <= ?";

			ps = conn.prepareStatement(query);
			ps.setTimestamp(1, startDate);
			ps.setTimestamp(2, endDate);

			rs = ps.executeQuery();

			while (rs.next()) {
				ArrayList<Object> row = new ArrayList<>();

				// We must add these in the EXACT order your Controller expects them
				row.add(rs.getInt("reservationID")); // 0
				row.add(rs.getInt("tableId")); // 1
				row.add(rs.getInt("numberOfDiners")); // 2
				row.add(rs.getInt("confirmationCode")); // 3
				row.add(rs.getInt("customerId")); // 4
				row.add(rs.getTimestamp("reservationDate")); // 5
				row.add(rs.getTimestamp("dateOfMakeReservation")); // 6
				row.add(rs.getTimestamp("arrivalTime")); // 7
				row.add(rs.getTimestamp("leavingTime")); // 8
				row.add(rs.getString("status")); // 9

				reservations.add(row);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return reservations;
	}

	/**
	 * Retrieves reservations matching a specific dynamic criterion.
	 * <p>
	 * <b>Security & Safety Measures:</b>
	 * <ul>
	 * <li><b>SQL Injection Prevention:</b> The column name (attribute) is validated
	 * against a hardcoded whitelist (Switch statement). Raw strings are never
	 * concatenated into the query.</li>
	 * <li><b>Type Validation:</b> The value parameter is explicitly checked to
	 * ensure it is either an <code>Integer</code> or a <code>String</code> before
	 * execution.</li>
	 * </ul>
	 * </p>
	 *
	 * @param attribute The name of the database column (e.g., "status", "tableId").
	 * @param value     The value to search for. Must be an Integer or a String.
	 * @return An ArrayList of ArrayLists containing the raw data, or null on
	 *         error/invalid type.
	 */
	public ArrayList<ArrayList<Object>> getReservationsByAttributeQuery(String attribute, Object value) {
		ArrayList<ArrayList<Object>> reservations = new ArrayList<>();

		// 1. Validate the value type immediately
		if (!(value instanceof Integer) && !(value instanceof String)) {
			System.out.println("Error: Invalid parameter type. 'value' must be Integer or String. Received: "
					+ (value == null ? "null" : value.getClass().getSimpleName()));
			return null;
		}

		PooledConnection pConn = this.getConnection();

		if (pConn == null) {
			return null;
		}

		Connection conn = pConn.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			String query = "";

			// 2. SECURITY MEASURES: Whitelist the column names (Prevents SQL Injection)
			// We cannot use '?' for column identifiers in PreparedStatement, so we use a
			// switch
			// to ensure only valid, pre-defined column names are used in the query string.
			switch (attribute) {
			case "status":
				query = "SELECT * FROM table_reservations WHERE status = ?";
				break;
			case "tableId":
				query = "SELECT * FROM table_reservations WHERE tableId = ?";
				break;
			case "numberOfDiners":
				query = "SELECT * FROM table_reservations WHERE numberOfDiners = ?";
				break;
			default:
				System.out.println("Error: Invalid attribute requested: " + attribute);
				return null;
			}

			ps = conn.prepareStatement(query);

			// 3. Set the parameter safely
			// Since we validated above that value is Integer or String, setObject will
			// correctly
			// map it to the SQL Types (INT or VARCHAR/ENUM).
			ps.setObject(1, value);

			rs = ps.executeQuery();

			while (rs.next()) {
				ArrayList<Object> row = new ArrayList<>();

				// Standard mapping
				row.add(rs.getInt("reservationID")); // 0
				row.add(rs.getInt("tableId")); // 1
				row.add(rs.getInt("numberOfDiners")); // 2
				row.add(rs.getInt("confirmationCode")); // 3
				row.add(rs.getInt("customerId")); // 4
				row.add(rs.getTimestamp("reservationDate")); // 5
				row.add(rs.getTimestamp("dateOfMakeReservation")); // 6
				row.add(rs.getTimestamp("arrivalTime")); // 7
				row.add(rs.getTimestamp("leavingTime")); // 8
				row.add(rs.getString("status")); // 9

				reservations.add(row);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResources(ps, rs);
			releaseConnection(pConn);
		}

		return reservations;
	}

}