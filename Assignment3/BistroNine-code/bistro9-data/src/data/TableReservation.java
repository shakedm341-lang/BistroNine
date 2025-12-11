package data;

import java.sql.Timestamp;

public class TableReservation  
{
	
	private int reservationId;
	private Timestamp ReservationDate;
	private int numberOfDiners;
	private int confirmationCode;
	private int subscriberId;
	private Timestamp DateOfMakeReservation;
	private Timestamp arrivalTime;
	private Timestamp leavingTime;
	private int tableId;
	

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

	public int getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(int subscriberId) {
		this.subscriberId = subscriberId;
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

	
}
