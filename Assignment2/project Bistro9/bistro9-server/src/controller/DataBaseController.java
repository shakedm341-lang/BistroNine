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

	// Private constructor for Singleton for one connection to DB
	private DataBaseController() {
		createConnectionToDB();
	}
	//Returns the only object opened from this class
	public static DataBaseController getInstance() {
		if (instance == null) {
			instance = new DataBaseController();
		}
		return instance;
	}
	//Communicates with the DB
	public void createConnectionToDB() {
		try {
			connectionToDB = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/sys?allowLoadLocalInfile=true&serverTimezone=Asia/Jerusalem&useSSL=false",
					"root", "Aa123456");
			System.out.println("SQL connection succeed");
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	/**
	 * Gets all reservations from the DB.
	 */
	public ArrayList<String> getAllReservationsQuery() {
		ArrayList<String> reservationsList = new ArrayList<>();

		if (connectionToDB == null)
			return reservationsList;

		PreparedStatement ps = null;
		ResultSet rs = null;

		try {//Creating a query that returns all orders in the DB
			String query = "SELECT * FROM tablereservations";
			ps = connectionToDB.prepareStatement(query);
			rs = ps.executeQuery();

			while (rs.next()) {//Inserts order details as strings into the order list
				String reservation = new String();

				StringBuilder sb = new StringBuilder();

				sb.append(rs.getString("reservationID"));
				sb.append(",");
				sb.append(rs.getString("ReservationDate"));
				sb.append(",");
				sb.append(rs.getString("numberOfDiners"));
				sb.append(",");
				sb.append(rs.getString("confirmationCode"));
				sb.append(",");
				sb.append(rs.getString("subscriberId"));
				sb.append(",");
				sb.append(rs.getString("DateOfMakeReservation"));

				reservation = sb.toString();

				reservationsList.add(reservation);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return reservationsList;//Returns the list of orders to the reservationController as a list of strings
	}

	/**
	 * Updates the Date and NumberOfDiners for a specific ID.
	 */
	public boolean updateReservationDetailsQuery(TableReservation t) {
		if (connectionToDB == null)
			return false;

		PreparedStatement ps = null;
		try {//Create a query to update existing order details
			String sql = "UPDATE tablereservations SET ReservationDate = ?, numberOfDiners = ? WHERE reservationID = ?";

			ps = connectionToDB.prepareStatement(sql);
			ps.setString(1, t.getReservationDate());
			ps.setInt(2, t.getNumberOfDiners());
			ps.setInt(3, t.getReservationId());

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;//Returns whether the update was successful or not to the reservationController 

		} catch (SQLException e) {
			e.printStackTrace();
			return false;//Returns whether the update was successful or not to the reservationController 
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}