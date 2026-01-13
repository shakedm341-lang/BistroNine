package data;

import java.sql.Timestamp;

/**
 * Entity class representing an entry in the restaurant's waiting list.
 * Tracks customers waiting for a table, including their entry/exit times, 
 * party size, and current status.
 */
public class WaitList 
{
	private int waitingId ;// AUTO_INCREMENT in DB//
	private int reservationId;
	private int numberOfDiners;//
    private Timestamp  entryTimeToList ;//DEFAULT CURRENT_TIMESTAMP in DB//
    private Timestamp exitTimeFromList;
    private String status ; //waiting, seated, cancelled //DEFAULT 'waiting', in DB//
    private String type ; //walk_in, check_in//
	

	/**
	 * Default constructor for WaitList.
	 */
	public WaitList() 
	{
		// TODO Auto-generated constructor stub
	}

	/**
	 * Gets the unique identifier for the waitlist entry.
	 * @return The waiting ID.
	 */
	public int getWaitingId() {
		return waitingId;
	}

	/**
	 * Sets the unique identifier for the waitlist entry.
	 * @param waiting_id The ID to set.
	 */
	public void setWaitingId(int waiting_id) {
		this.waitingId = waiting_id;
	}

	/**
	 * Gets the current status of the customer on the list (e.g., 'waiting', 'seated', 'cancelled').
	 * @return The current status.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Sets the status of the customer on the list.
	 * @param status The status string to set.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Gets the ID of the associated reservation.
	 * @return The reservation ID.
	 */
	public int getReservationId() {
		return reservationId;
	}

	/**
	 * Sets the ID of the associated reservation.
	 * @param reservationId The reservation ID to set.
	 */
	public void setReservationId(int reservationId) {
		this.reservationId = reservationId;
	}

	/**
	 * Gets the timestamp when the customer was added to the waiting list.
	 * @return The entry timestamp.
	 */
	public Timestamp getEntryTimeToList() {
		return entryTimeToList;
	}

	/**
	 * Sets the timestamp for when the customer was added to the waiting list.
	 * @param entryTimeToList The entry timestamp to set.
	 */
	public void setEntryTimeToList(Timestamp entryTimeToList) {
		this.entryTimeToList = entryTimeToList;
	}

	/**
	 * Gets the type of waitlist entry (e.g., 'walk_in', 'check_in').
	 * @return The entry type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of waitlist entry.
	 * @param type The type string to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the number of diners in the party.
	 * @return The number of diners.
	 */
	public int getNumberOfDiners() {
		return numberOfDiners;
	}

	/**
	 * Sets the number of diners in the party.
	 * @param numberOfDiners The number of diners to set.
	 */
	public void setNumberOfDiners(int numberOfDiners) {
		this.numberOfDiners = numberOfDiners;
	}

	/**
	 * Gets the timestamp when the customer was removed from the waiting list.
	 * @return The exit timestamp.
	 */
	public Timestamp getExitTimeFromList() {
		return exitTimeFromList;
	}

	/**
	 * Sets the timestamp for when the customer was removed from the waiting list.
	 * @param exitTimeFromList The exit timestamp to set.
	 */
	public void setExitTimeFromList(Timestamp exitTimeFromList) {
		this.exitTimeFromList = exitTimeFromList;
	}
}