package gui;

/**
 * Interface for handling the response of a reservation deletion request.
 * Components that initiate a deletion should implement this to receive feedback.
 */
public interface IReservationDeleter {
	
	/**
	 * Callback method triggered when the server responds to a reservation deletion request.
	 * 
	 * @param isDeleted true if the reservation was successfully deleted, false otherwise.
	 */
	public void handleDeleteReservationResponse(boolean isDeleted);

}
