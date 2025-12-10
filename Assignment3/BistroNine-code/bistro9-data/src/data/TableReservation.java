package data;

import java.io.Serializable;
import java.sql.Timestamp;

public class TableReservation  {
	private static final long serialVersionUID = 1L;

	private int reservationId;
	private Timestamp ReservationDate;
	private int numberOfDiners;
	private int confirmationCode;
	private int subscriberId;
	private Timestamp DateOfMakeReservation;

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
}
