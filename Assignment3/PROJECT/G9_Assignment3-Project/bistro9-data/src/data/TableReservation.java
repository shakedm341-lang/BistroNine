package data;

import java.sql.Timestamp;

/**
 * Entity class representing a table reservation in the system.
 * Tracks the full lifecycle of a booking, including the associated customer, 
 * table assignment, timing, and current status.
 */
public class TableReservation  
{
	
	private int reservationId;//giveing by DB auto increment
	private int tableId;
	private int numberOfDiners;
	private int confirmationCode;
	private int customerId;
	private Timestamp ReservationDate;
	private Timestamp DateOfMakeReservation;//giveing by DB auto CURRENT_TIMESTAMP
	private Timestamp arrivalTime;
	private Timestamp leavingTime;
	private String status;//reset as active in DB  //'active','arrived' ,'cancelled', 'completed','waiting'
	
       
	/**
	 * Default constructor for TableReservation.
	 */
	public TableReservation() {

	}

	/**
	 * Gets the unique reservation identifier.
	 * @return The reservation ID.
	 */
	public int getReservationId() {
		return reservationId;
	}

	/**
	 * Sets the unique reservation identifier.
	 * @param reservationId The ID to set.
	 */
	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	/**
	 * Gets the scheduled date and time for the reservation.
	 * @return The reservation timestamp.
	 */
	public Timestamp getReservationDate() {
		return ReservationDate;
	}

	/**
	 * Sets the scheduled date and time for the reservation.
	 * @param timestamp The scheduled timestamp to set.
	 */
	public void setReservationDate(Timestamp timestamp) {
		ReservationDate = timestamp;
	}

	/**
	 * Gets the number of diners expected for this reservation.
	 * @return The number of diners.
	 */
	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	/**
	 * Sets the number of diners expected for this reservation.
	 * @param numberOfDiners The number of diners to set.
	 */
	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	/**
	 * Gets the unique confirmation code for the reservation.
	 * @return The confirmation code.
	 */
	public int getConfirmationCode() {
		return confirmationCode;
	}

	/**
	 * Sets the unique confirmation code for the reservation.
	 * @param confirmationCode The code to set.
	 */
	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	/**
	 * Gets the timestamp indicating when the reservation was created in the system.
	 * @return The creation date timestamp.
	 */
	public Timestamp getDateOfMakeReservation() {
		return DateOfMakeReservation;
	}

	/**
	 * Sets the timestamp indicating when the reservation was created.
	 * @param dateOfMakeReservation The creation timestamp to set.
	 */
	public void setDateOfMakeReservation(Timestamp dateOfMakeReservation) {
		DateOfMakeReservation = dateOfMakeReservation;
	}

	/**
	 * Gets the actual time the customer arrived at the restaurant.
	 * @return The arrival timestamp.
	 */
	public Timestamp getArrivalTime() {
		return arrivalTime;
	}

	/**
	 * Sets the actual time the customer arrived at the restaurant.
	 * @param arrivalTime The arrival timestamp to set.
	 */
	public void setArrivalTime(Timestamp arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	/**
	 * Gets the time the customer left the restaurant.
	 * @return The leaving timestamp.
	 */
	public Timestamp getLeavingTime() {
		return leavingTime;
	}

	/**
	 * Sets the time the customer left the restaurant.
	 * @param leavingTime The leaving timestamp to set.
	 */
	public void setLeavingTime(Timestamp leavingTime) {
		this.leavingTime = leavingTime;
	}

	/**
	 * Gets the ID of the table assigned to this reservation.
	 * @return The table ID.
	 */
	public int getTableId() {
		return tableId;
	}

	/**
	 * Sets the ID of the table assigned to this reservation.
	 * @param tableId The table ID to set.
	 */
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	/**
	 * Gets the current status of the reservation (e.g., 'active', 'arrived', 'cancelled').
	 * @return The status string.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the current status of the reservation.
	 * @param status The status string to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gets the ID of the customer who made the reservation.
	 * @return The customer ID.
	 */
	public int getCustomerId() {
		return customerId;
	}

	/**
	 * Sets the ID of the customer who made the reservation.
	 * @param customerId The customer ID to set.
	 */
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}
}