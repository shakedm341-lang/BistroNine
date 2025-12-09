package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import data.TableReservation;

public class DataBaseController {

	private static DataBaseController instance;
	private Connection connectionToDB;
	private static String dbPassword; 

	// Private constructor calls the connection method
	private DataBaseController() {
		createConnectionToDB();
	}

	/**
	 * NEW METHOD: Initializes the singleton with the password.
	 * Renamed to initiateDBC to avoid confusion with network clients.
	 */
	public static void initiateDBC(String password) {
		dbPassword = password;
		if (instance == null) {
			instance = new DataBaseController();
		}
	}

	public static DataBaseController getInstance() {
		return instance;
	}

	public void createConnectionToDB() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			// I added '&allowPublicKeyRetrieval=true' inside this long string.
			// This tells the driver it's okay to request the key from the server.
			connectionToDB = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/restaurant_db?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false",
					"root", dbPassword);
					
			System.out.println("SQL connection succeed");
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public ArrayList<String> getAllReservationsQuery() {
		ArrayList<String> reservationsList = new ArrayList<>();
		if (connectionToDB == null) return reservationsList;
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String query = "SELECT * FROM tablereservations";
			ps = connectionToDB.prepareStatement(query);
			rs = ps.executeQuery();
			while (rs.next()) {
				StringBuilder sb = new StringBuilder();
				sb.append(rs.getString("reservationID")).append(",");
				sb.append(rs.getString("ReservationDate")).append(",");
				sb.append(rs.getString("numberOfDiners")).append(",");
				sb.append(rs.getString("confirmationCode")).append(",");
				sb.append(rs.getString("subscriberId")).append(",");
				sb.append(rs.getString("DateOfMakeReservation"));
				reservationsList.add(sb.toString());
			}
		} catch (SQLException e) { e.printStackTrace(); }
		return reservationsList;
	}

	public boolean updateReservationDetailsQuery(TableReservation t) {
		if (connectionToDB == null) return false;
		try {
			String sql = "UPDATE tablereservations SET ReservationDate = ?, numberOfDiners = ? WHERE reservationID = ?";
			PreparedStatement ps = connectionToDB.prepareStatement(sql);
			ps.setString(1, t.getReservationDate());
			ps.setInt(2, t.getNumberOfDiners());
			ps.setInt(3, t.getReservationId());
			return ps.executeUpdate() > 0;
		} catch (SQLException e) { e.printStackTrace(); return false; }
	}
}