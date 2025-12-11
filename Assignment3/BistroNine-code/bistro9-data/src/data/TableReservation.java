package data;

import java.sql.Timestamp;

public class TableReservation  
{
	
	private int reservationId;//giveing by DB auto increment
	private Timestamp ReservationDate;
	private int numberOfDiners;
	private int confirmationCode;
	private int phoneNumber;
	private Timestamp DateOfMakeReservation;//giveing by DB auto CURRENT_TIMESTAMP
	private Timestamp arrivalTime;
	private Timestamp leavingTime;
	private int tableId;
	private String status;//reset as active in DB
	

	
	public TableReservation() {

	}

	public int getReservationId() {
		return reservationId;
	}

	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	public Timestamp getReservationDate() {
		return ReservationDate;
	}

	public void setReservationDate(Timestamp timestamp) {
		ReservationDate = timestamp;
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


	public Timestamp getDateOfMakeReservation() {
		return DateOfMakeReservation;
	}

	public void setDateOfMakeReservation(Timestamp dateOfMakeReservation) {
		DateOfMakeReservation = dateOfMakeReservation;
	}
	public Timestamp getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(Timestamp arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public Timestamp getLeavingTime() {
		return leavingTime;
	}

	public void setLeavingTime(Timestamp leavingTime) {
		this.leavingTime = leavingTime;
	}

	public int getTableId() {
		return tableId;
	}

	public void setTableId(int tableId) {
		this.tableId = tableId;
	}

	public int getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(int phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	
}
