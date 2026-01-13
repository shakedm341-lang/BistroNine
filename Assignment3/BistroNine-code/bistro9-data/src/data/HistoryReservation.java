package data;

import java.sql.Timestamp;

/**
 * Entity class representing the historical record of a reservation.
 */
public class HistoryReservation 
{
	private int reservationId;
	private int tableId;
	private int numberOfDiners;
	private int confirmationCode;
	private Timestamp ReservationDate;
	private Timestamp DateOfMakeReservation;
	private String status;
	private double totalAmountAfterDiscount;
	private double discountSize;
	private String paymentMethod ;
	
	/**
	 * Default constructor for HistoryReservation.
	 */
	public HistoryReservation() 
	{
		
	}

	/**
	 * Gets the table ID assigned to the reservation.
	 * @return The table ID.
	 */
	public int getTableId() {
		return tableId;
	}

	/**
	 * Sets the table ID.
	 * @param tableId The table ID to set.
	 */
	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	/**
	 * Gets the number of diners.
	 * @return Number of diners.
	 */
	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	/**
	 * Sets the number of diners.
	 * @param numberOfDiners The count of diners to set.
	 */
	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	/**
	 * Gets the confirmation code.
	 * @return The confirmation code.
	 */
	public int getConfirmationCode() {
		return confirmationCode;
	}

	/**
	 * Sets the confirmation code.
	 * @param confirmationCode The code to set.
	 */
	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	/**
	 * Gets the scheduled reservation date.
	 * @return Timestamp of the reservation.
	 */
	public Timestamp getReservationDate() {
		return ReservationDate;
	}

	/**
	 * Sets the scheduled reservation date.
	 * @param reservationDate The scheduled timestamp.
	 */
	public void setReservationDate(Timestamp reservationDate) {
		ReservationDate = reservationDate;
	}

	/**
	 * Gets the timestamp of when the reservation was made.
	 * @return The creation date timestamp.
	 */
	public Timestamp getDateOfMakeReservation() {
		return DateOfMakeReservation;
	}

	/**
	 * Sets the timestamp of when the reservation was made.
	 * @param dateOfMakeReservation The creation timestamp.
	 */
	public void setDateOfMakeReservation(Timestamp dateOfMakeReservation) {
		DateOfMakeReservation = dateOfMakeReservation;
	}

	/**
	 * Gets the final status of the reservation.
	 * @return The status string.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the status of the reservation.
	 * @param status The status to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gets the total amount paid after discount.
	 * @return The final total amount.
	 */
	public double getTotalAmountAfterDiscount() {
		return totalAmountAfterDiscount;
	}

	/**
	 * Sets the total amount after discount.
	 * @param totalAmountAfterDiscount The final amount.
	 */
	public void setTotalAmountAfterDiscount(double totalAmountAfterDiscount) {
		this.totalAmountAfterDiscount = totalAmountAfterDiscount;
	}

	/**
	 * Gets the payment method used.
	 * @return The payment method as a string.
	 */
	public String getPaymentMethod() {
		return paymentMethod;
	}

	/**
	 * Sets the payment method.
	 * @param paymentMethod The method to set.
	 */
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	/**
	 * Gets the value of the discount applied.
	 * @return The discount size.
	 */
	public double getDiscountSize() {
		return discountSize;
	}

	/**
	 * Sets the value of the discount.
	 * @param discountSize The discount amount to set.
	 */
	public void setDiscountSize(double discountSize) {
		this.discountSize = discountSize;
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

}