package data;

import java.sql.Timestamp;

public class HistoryReservation 
{

	private int tableId;
	private int numberOfDiners;
	private int confirmationCode;
	private Timestamp ReservationDate;
	private Timestamp DateOfMakeReservation;
	private String status;
	private double totalAmountAfterDiscount;
	private double discountSize;
	private String paymentMethod ;
	
	public HistoryReservation() 
	{
		
	}

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public Timestamp getReservationDate() {
		return ReservationDate;
	}

	public void setReservationDate(Timestamp reservationDate) {
		ReservationDate = reservationDate;
	}

	public Timestamp getDateOfMakeReservation() {
		return DateOfMakeReservation;
	}

	public void setDateOfMakeReservation(Timestamp dateOfMakeReservation) {
		DateOfMakeReservation = dateOfMakeReservation;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public double getTotalAmountAfterDiscount() {
		return totalAmountAfterDiscount;
	}

	public void setTotalAmountAfterDiscount(double totalAmountAfterDiscount) {
		this.totalAmountAfterDiscount = totalAmountAfterDiscount;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public double getDiscountSize() {
		return discountSize;
	}

	public void setDiscountSize(double discountSize) {
		this.discountSize = discountSize;
	}

}
